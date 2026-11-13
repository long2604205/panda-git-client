@echo off
call "%~dp0..\bazel.cmd" run //build:panda_git_build %*
exit /b %errorlevel%
