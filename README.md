# scala-file-utilities

Some useful Scala file utilities and extras not found other places.

This repo is still under development...

## TraversablePaths

A Traversable That Walks the File System. Currently uses java.nio.Files.walkFileTree, but more Scala friendly in terms of a Traversable collection.

## Duplicate Finder

Utilities for Finding Duplicate Files. Traverse the file system looking for duplicate files, and return all groups of duplicate files found. As much as possible, comparisons are performed in parallel to improve performance.
