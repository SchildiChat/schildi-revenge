This is a kotlin / compose multiplatform project, targetting desktop / JVM - *not* Android.

The matrix-rust-sdk directory contains the employed Rust SDK, no need to search for gradle caches to inspect its code.
If you need access to ruma, you may have luck at ../ruma.

Files in the matrix-rust-sdk directory commonly need to be easy to do upstream merges with, so you shouldn't touch them
if not necessary, and only do minimal changes to existing files if not avoidable. Creating new files or patching
SchildiChat-specific files (by package name or by having an "Sc" prefix in the filename) should be fine.
