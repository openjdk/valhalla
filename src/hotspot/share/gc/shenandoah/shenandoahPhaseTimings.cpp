/*
 * Copyright (c) 2017, 2020, Red Hat, Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#include "precompiled.hpp"

#include "gc/shared/workerDataArray.inline.hpp"
#include "gc/shenandoah/shenandoahCollectorPolicy.hpp"
#include "gc/shenandoah/shenandoahPhaseTimings.hpp"
#include "gc/shenandoah/shenandoahHeap.hpp"
#include "gc/shenandoah/shenandoahHeuristics.hpp"
#include "gc/shenandoah/shenandoahUtils.hpp"
#include "runtime/orderAccess.hpp"
#include "utilities/ostream.hpp"

#define SHENANDOAH_PHASE_NAME_FORMAT "%-28s"
#define SHENANDOAH_S_TIME_FORMAT "%8.3lf"
#define SHENANDOAH_US_TIME_FORMAT "%8.0lf"
#define SHENANDOAH_US_WORKER_TIME_FORMAT "%3.0lf"

#define GC_PHASE_DECLARE_NAME(type, title) \
  title,

const char* ShenandoahPhaseTimings::_phase_names[] = {
  SHENANDOAH_GC_PHASE_DO(GC_PHASE_DECLARE_NAME)
};

#undef GC_PHASE_DECLARE_NAME

ShenandoahPhaseTimings::ShenandoahPhaseTimings(uint max_workers) :
  _max_workers(max_workers) {
  assert(_max_workers > 0, "Must have some GC threads");

  // Initialize everything to sane defaults
  for (uint i = 0; i < _num_phases; i++) {
#define SHENANDOAH_WORKER_DATA_NULL(type, title) \
    _worker_data[i] = NULL;
    SHENANDOAH_GC_PAR_PHASE_DO(,, SHENANDOAH_WORKER_DATA_NULL)
#undef SHENANDOAH_WORKER_DATA_NULL
    _cycle_data[i] = 0;
  }

  // Then punch in the worker-related data.
  // Every worker phase get a bunch of internal objects, except
  // the very first slot, which is "<total>" and is not populated.
  for (uint i = 0; i < _num_phases; i++) {
    if (is_worker_phase(Phase(i))) {
      int c = 0;
#define SHENANDOAH_WORKER_DATA_INIT(type, title) \
      if (c++ != 0) _worker_data[i + c] = new ShenandoahWorkerData(title, _max_workers);
      SHENANDOAH_GC_PAR_PHASE_DO(,, SHENANDOAH_WORKER_DATA_INIT)
#undef SHENANDOAH_WORKER_DATA_INIT
    }
  }

  _policy = ShenandoahHeap::heap()->shenandoah_policy();
  assert(_policy != NULL, "Can not be NULL");

  _current_worker_phase = _invalid_phase;
}

ShenandoahPhaseTimings::Phase ShenandoahPhaseTimings::worker_par_phase(Phase phase, GCParPhases par_phase) {
  assert(is_worker_phase(phase), "Phase should accept worker phase times: %s", phase_name(phase));
  Phase p = Phase(phase + 1 + par_phase);
  assert(p >= 0 && p < _num_phases, "Out of bound for: %s", phase_name(phase));
  return p;
}

ShenandoahWorkerData* ShenandoahPhaseTimings::worker_data(Phase phase, GCParPhases par_phase) {
  Phase p = worker_par_phase(phase, par_phase);
  ShenandoahWorkerData* wd = _worker_data[p];
  assert(wd != NULL, "Counter initialized: %s", phase_name(p));
  return wd;
}

bool ShenandoahPhaseTimings::is_worker_phase(Phase phase) {
  assert(phase >= 0 && phase < _num_phases, "Out of bounds");
  switch (phase) {
    case init_evac:
    case scan_roots:
    case update_roots:
    case final_update_refs_roots:
    case full_gc_scan_roots:
    case full_gc_update_roots:
    case full_gc_adjust_roots:
    case degen_gc_update_roots:
    case full_gc_purge_class_unload:
    case full_gc_purge_weak_par:
    case purge_class_unload:
    case purge_weak_par:
    case heap_iteration_roots:
      return true;
    default:
      return false;
  }
}

void ShenandoahPhaseTimings::set_cycle_data(Phase phase, double time) {
#ifdef ASSERT
  double d = _cycle_data[phase];
  assert(d == 0, "Should not be set yet: %s, current value: %lf", phase_name(phase), d);
#endif
  _cycle_data[phase] = time;
}

void ShenandoahPhaseTimings::record_phase_time(Phase phase, double time) {
  if (!_policy->is_at_shutdown()) {
    set_cycle_data(phase, time);
  }
}

void ShenandoahPhaseTimings::record_workers_start(Phase phase) {
  assert(is_worker_phase(phase), "Phase should accept worker phase times: %s", phase_name(phase));

  assert(_current_worker_phase == _invalid_phase, "Should not be set yet: requested %s, existing %s",
         phase_name(phase), phase_name(_current_worker_phase));
  _current_worker_phase = phase;

  for (uint i = 1; i < GCParPhasesSentinel; i++) {
    worker_data(phase, GCParPhases(i))->reset();
  }
}

void ShenandoahPhaseTimings::record_workers_end(Phase phase) {
  assert(is_worker_phase(phase), "Phase should accept worker phase times: %s", phase_name(phase));
  _current_worker_phase = _invalid_phase;
}

void ShenandoahPhaseTimings::flush_par_workers_to_cycle() {
  for (uint pi = 0; pi < _num_phases; pi++) {
    Phase phase = Phase(pi);
    if (is_worker_phase(phase)) {
      double s = 0;
      for (uint i = 1; i < GCParPhasesSentinel; i++) {
        double t = worker_data(phase, GCParPhases(i))->sum();
        // add to each line in phase
        set_cycle_data(Phase(phase + i + 1), t);
        s += t;
      }
      // add to total for phase
      set_cycle_data(Phase(phase + 1), s);
    }
  }
}

void ShenandoahPhaseTimings::flush_cycle_to_global() {
  for (uint i = 0; i < _num_phases; i++) {
    _global_data[i].add(_cycle_data[i]);
    _cycle_data[i] = 0;
  }
  OrderAccess::fence();
}

void ShenandoahPhaseTimings::print_cycle_on(outputStream* out) const {
  out->cr();
  out->print_cr("All times are wall-clock times, except per-root-class counters, that are sum over");
  out->print_cr("all workers. Dividing the <total> over the root stage time estimates parallelism.");
  out->cr();
  for (uint i = 0; i < _num_phases; i++) {
    double v = _cycle_data[i] * 1000000.0;
    if (v > 0) {
      out->print(SHENANDOAH_PHASE_NAME_FORMAT " " SHENANDOAH_US_TIME_FORMAT " us", _phase_names[i], v);
      if (_worker_data[i] != NULL) {
        out->print(", workers (us): ");
        for (uint c = 0; c < _max_workers; c++) {
          double tv = _worker_data[i]->get(c);
          if (tv != ShenandoahWorkerData::uninitialized()) {
            out->print(SHENANDOAH_US_WORKER_TIME_FORMAT ", ", tv * 1000000.0);
          } else {
            out->print("%3s, ", "---");
          }
        }
      }
      out->cr();
    }
  }
}

void ShenandoahPhaseTimings::print_global_on(outputStream* out) const {
  out->cr();
  out->print_cr("GC STATISTICS:");
  out->print_cr("  \"(G)\" (gross) pauses include VM time: time to notify and block threads, do the pre-");
  out->print_cr("        and post-safepoint housekeeping. Use -Xlog:safepoint+stats to dissect.");
  out->print_cr("  \"(N)\" (net) pauses are the times spent in the actual GC code.");
  out->print_cr("  \"a\" is average time for each phase, look at levels to see if average makes sense.");
  out->print_cr("  \"lvls\" are quantiles: 0%% (minimum), 25%%, 50%% (median), 75%%, 100%% (maximum).");
  out->cr();
  out->print_cr("  All times are wall-clock times, except per-root-class counters, that are sum over");
  out->print_cr("  all workers. Dividing the <total> over the root stage time estimates parallelism.");
  out->cr();

  for (uint i = 0; i < _num_phases; i++) {
    if (_global_data[i].maximum() != 0) {
      out->print_cr(SHENANDOAH_PHASE_NAME_FORMAT " = " SHENANDOAH_S_TIME_FORMAT " s "
                    "(a = " SHENANDOAH_US_TIME_FORMAT " us) "
                    "(n = " INT32_FORMAT_W(5) ") (lvls, us = "
                    SHENANDOAH_US_TIME_FORMAT ", "
                    SHENANDOAH_US_TIME_FORMAT ", "
                    SHENANDOAH_US_TIME_FORMAT ", "
                    SHENANDOAH_US_TIME_FORMAT ", "
                    SHENANDOAH_US_TIME_FORMAT ")",
                    _phase_names[i],
                    _global_data[i].sum(),
                    _global_data[i].avg() * 1000000.0,
                    _global_data[i].num(),
                    _global_data[i].percentile(0) * 1000000.0,
                    _global_data[i].percentile(25) * 1000000.0,
                    _global_data[i].percentile(50) * 1000000.0,
                    _global_data[i].percentile(75) * 1000000.0,
                    _global_data[i].maximum() * 1000000.0
      );
    }
  }
}

ShenandoahWorkerTimingsTracker::ShenandoahWorkerTimingsTracker(ShenandoahPhaseTimings::GCParPhases par_phase, uint worker_id) :
        _timings(ShenandoahHeap::heap()->phase_timings()), _phase(_timings->current_worker_phase()),
        _par_phase(par_phase), _worker_id(worker_id) {
  assert(_timings->worker_data(_phase, _par_phase)->get(_worker_id) == ShenandoahWorkerData::uninitialized(),
         "Should not be set yet: %s", ShenandoahPhaseTimings::phase_name(_timings->worker_par_phase(_phase, _par_phase)));
  _start_time = os::elapsedTime();
}

ShenandoahWorkerTimingsTracker::~ShenandoahWorkerTimingsTracker() {
  _timings->worker_data(_phase, _par_phase)->set(_worker_id, os::elapsedTime() - _start_time);

  if (ShenandoahGCPhase::is_root_work_phase()) {
    ShenandoahPhaseTimings::Phase root_phase = ShenandoahGCPhase::current_phase();
    ShenandoahPhaseTimings::Phase cur_phase = _timings->worker_par_phase(root_phase, _par_phase);
    _event.commit(GCId::current(), _worker_id, ShenandoahPhaseTimings::phase_name(cur_phase));
  }
}

