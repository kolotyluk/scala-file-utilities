/*  Copyright © 2014 by Eric Kolotyluk <eric@kolotyluk.net>

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package net.kolotyluk.scala.files

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult
import java.nio.file.SimpleFileVisitor
import java.nio.file.Files.walkFileTree
import java.nio.file.FileVisitResult.CONTINUE
import java.nio.file.Path
import java.nio.file.FileVisitOption.FOLLOW_LINKS
import java.util.EnumSet
import scala.annotation.tailrec
import net.kolotyluk.java.files.Files
import java.io.IOException
import java.nio.file.FileVisitOption

/** A Traversable That Walks the File System
  * 
  * Currently uses java.nio.Files.walkFileTree, but more Scala friendly in terms of a Traversable collection.
  * 
  * Also includes some useful utilities, such as [[duplicateFilesList]]
  * 
  * @author Eric Kolotyluk
  * 
  * @constructor constructs a new Traversable[Path]
  * @param roots : Path*<br>the files or directories that are traversed
  * @param traverseDirectories : Boolean<br><code>true</code> if you want to visit directories. Default is <code>false</code>
  * @param traverseFiles : Boolean<br><code>true</code> if you want to visit files. Default is <code>true</code>
  * @param traverseLinks : Boolean<br><code>true</code> if you want to traverse through symbolic links. Default is <code>true</code>.
  */
case class TraversablePaths(roots: Path*)
  (traverseDirectoriesDown: Boolean = false, traverseDirectoriesUp: Boolean = false, traverseFiles: Boolean = true, traverseLinks: Boolean = true, traverseFailures: Boolean = false)
  extends Traversable[Path] {
  
  /** Concrete foreach implementation.
    *  
    * foreach is the basic method of traversing Scala collections,
    * enabling Traversable to support composable monadic operations
    * such as filter, fold, map, etc.
    * 
    * java.nio.Files.walkFileTree does most of the heavy lifting,
    * but we try to make it more Scala friendly.
    * 
    * See also [[http://www.scala-lang.org/api/current/index.html#scala.collection.Traversable]]
    */
  override def foreach[U](f: Path => U) {
    class visitor extends SimpleFileVisitor[Path] {
      override def preVisitDirectory(path: Path, attrs: BasicFileAttributes) : FileVisitResult = {
        if (traverseDirectoriesDown) f(path)
        CONTINUE
      }
      override def postVisitDirectory(path: Path, e: IOException) : FileVisitResult = {
        if (traverseDirectoriesUp) f(path)
        CONTINUE
      }
      override def visitFile(path: Path, attrs: BasicFileAttributes) : FileVisitResult = {
        if (traverseFiles) f(path)
        CONTINUE
      }
      override def visitFileFailed(path: Path, e: IOException) : FileVisitResult = {
        if (traverseFailures) f(path)
        CONTINUE
      }
    }
    
    // Walk a tree of infinite depth, following symbolic links if requested.
    // NIO keeps track of symbolic links so that cycles are not
    // followed indefinitely.
    if (traverseLinks)
      roots.foreach(walkFileTree(_, EnumSet.of(FOLLOW_LINKS), Integer.MAX_VALUE, new visitor))
    else
     roots.foreach(walkFileTree(_, new visitor))
  }

}
