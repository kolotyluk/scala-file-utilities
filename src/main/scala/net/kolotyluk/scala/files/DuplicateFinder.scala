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

import java.nio.file.Path
import scala.annotation.tailrec
import net.kolotyluk.java.files.Files

/** Utilities for Finding Duplicate Files
  *  
  * Traverse the file system looking for duplicate files, and return all
  * groups of duplicate files found. As much as possible, comparisons are
  * performed in parallel to improve performance.
  *  
  * Two or more files are considered duplicates if a byte-by-byte comparison
  * indicates the contents are identical.
  * 
  * Note: many modern file systems allow for 'hard' links where two or more entries
  * in a file system directory point to the same file on the files system. In general,
  * it is difficult to detect such a case.
  * 
  * Note: many modern files systems allow for symbolic or 'soft' links, where a file
  * system directory entry contains the path to file or other directory on the file
  * system. In general, it is easy to detect such a case.
  * 
  * ==Use Case: Media Files==
  * 
  * Duplicate files are common when people collect media files such as pictures,
  * music, videos, etc. It is often desirable to detect and delete duplicates in
  * order to reduce space on the file system.
  * {{{
  *   import java.nio.file.Files.delete
  *   val music = FileSystems.getDefault().getPath("D:\\Users\\Eric\\Google Drive\\Music")
  *   val duplicates = DuplicateFinder(music)().findFiles
  *   // delete only the tail, so we don't delete all our files
  *   duplicates.foreach(_.tail.foreach(delete))
  * }}}
  * 
  * ==Use Case: Google Drive Comparison==
  * 
  * Google Drive can get very upset when the underlying file system becomes disconnected,
  * and sometimes you end up having to start over and resync from the cloud. As a result,
  * you can get multiple versions of your local Google Drive folder, with minor differences.
  * Let's say you want to delete the duplicate files from the second folder:
  * {{{
  *   def delete(path : Path) {
  *     try {
  *       println("deleting " + path)
  *       java.nio.file.Files.delete(path)
  *     } catch {
  *       case exception: Exception => System.err.println(exception)
  *     }
  *   }
  *   
  *   val google1 = FileSystems.getDefault().getPath("D:\\Users\\Eric\\Google Drive")
  *   val google2 = FileSystems.getDefault().getPath("D:\\Users\\Eric\\Google Drive (Original)")
  *   
  *   val duplicates = DuplicateFinder(google1, google2)().findFiles
  *   println("deleting duplicate files")
  *   duplicates.foreach(_.filter(!_.startsWith(google1)).foreach(delete))
  * }}}
  * 
  * @author Eric Kolotyluk
  * 
  * @constructor constructs a new DuplicateFinder
  * @param roots : Path* -- the files or directories that are scanned
  * @param traverseLinks : Boolean -- true if you want to follow symbolic links
  */

case class DuplicateFinder(roots: Path*)(traverseLinks: Boolean = true) {
  
  /** Define the TraversablePaths
    *
    * TraversablePaths is a very powerful utility,
    * so we have to limit the caller's options to
    * make sure we don't do something stupid.
    */
  val traversablePaths = TraversablePaths(roots: _*)(
    traverseDirectoriesDown = false,
    traverseDirectoriesUp = false,
    traverseFiles = true,
    traverseLinks = this.traverseLinks,
    traverseFailures = false)

  /** A Collection of Files with Identical Content.
    */
  type DuplicateFiles = List[Path]

  /** A Collection of Collections of Files with Identical Content
    *
    * This operation should be functionally pure, that is, side-effect free.
    * If any of the file un-mapping or closing operations fail, a RuntimeException
    * will be thrown to indicate there are side effects.
    * 
    * Note: this operation can be computationally expensive on large
    * file systems, with many large files.
    * @return List[List[Path]]
    */
  def findFiles : List[DuplicateFiles] = {
    // All the cliques with more than one member.
    // Cliques with only one member won't have duplicates.
    val crowds = cliques.filter(_._2.size > 1).map(_._2)
    
    // Spread the crowds into groups of files with identical content.
    // This can be done in parallel because each clique is independent of the others.
    crowds.par.flatMap(spreadDuplicates(_)).toList
  }

  /**
   * A collection of files that all have identical length.
   */
  type Clique = Traversable[Path]

  /**
   * The collection of Cliques.
   * 
   * Only files of the same length can ever be identical,
   * and getting file lengths from the file system is a cheap
   * and easy operation.
   */
  def cliques = 
    if (traversablePaths.isEmpty) Map.empty
    else traversablePaths.groupBy((path: Path) => path.toFile().length())

  /**
   * Spread the cliques into groups of duplicate files.
   */
  def spreadDuplicates(clique: Clique) : List[DuplicateFiles] = {
    
    /**
     * Utility function to iterate recursively over the duplicatesList.
     * Could this be done differently?
     */
    @tailrec
    def spreadIdenticals(clique: Clique, duplicatesList: List[DuplicateFiles]) : List[DuplicateFiles] = {
      if (clique.isEmpty) duplicatesList
      else spreadIdenticals(clique.tail, groupDuplicates(clique.head, List(), duplicatesList))
    }

    /** Add a candidate file to the group of identical files, where every member of that group
      * has identical content, or create a new group where it is the only one in the group. Note: we only
      * need to check the first file in any group because by definition the other files in the
      * group are duplicates too.
      * @parameter candidate a potential candidate for an existing group of duplicate files.
      * @parameter checked is a List of Lists that have been checked
      * @parameter unchecked is a List of Lists that have not been checked
      * @returns as List of Lists, where the inner Lists all contain duplicate files
      */
    @tailrec
    def groupDuplicates(candidate: Path, checked: List[DuplicateFiles], unchecked: List[DuplicateFiles]) : List[DuplicateFiles] = {
      
      // Adding items to the head of a list reuses the tail,
      // and moderates memory growth for collections. This may
      // not be obvious to people unfamiliar with immutable
      // data structures. Also, because candidate was recently
      // read by the file system, it will more likely be in the
      // cache, to be read again for the next comparison. EK

      if (unchecked.isEmpty)
        List(List(candidate))
      else if (Files.contentEquals(candidate, unchecked.head.head))
        checked ::: (candidate :: unchecked.head) :: unchecked.tail
      else
        groupDuplicates(candidate, unchecked.head :: checked, unchecked.tail)
    }

    // We don't care about any groups with only one member.
    if (clique.isEmpty) List()
    else spreadIdenticals(clique.tail, groupDuplicates(clique.head, List(), List())).filter(_.count(f => true) > 1)
  }
}