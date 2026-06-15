#!/usr/bin/env python3
"""Wrapper to run uipro search.py with proper sys.path setup."""
import os
import sys
import runpy

# Ensure the scripts directory is on sys.path so relative imports work
SCRIPTS_DIR = os.path.dirname(os.path.abspath(__file__))
if SCRIPTS_DIR not in sys.path:
    sys.path.insert(0, SCRIPTS_DIR)

# Run search.py as __main__
runpy.run_path(os.path.join(SCRIPTS_DIR, "search.py"), run_name="__main__")
