package net.kolotyluk.scala.files

import java.nio.file.FileSystems
import java.nio.file.Path

object Worksheet {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
  
/*
  case class TP(roots: Seq[Path], traverseDirectories: Boolean, traverseFiles: Boolean, traverseLinks: Boolean) extends Traversable[Path] {
    def traverseDirectories(value: Boolean): TP = this.copy(traverseDirectories = value)
    def traverseFiles(value: Boolean): TP = this.copy(traverseFiles = value)
    def traverseLinks(value: Boolean): TP = this.copy(traverseLinks = value)
    
    def apply(paths: Path*): TP = TP(
      paths,
      traverseDirectories = false,
      traverseFiles = true,
      traverseLinks = true
    )
    
    println("roots = " + roots)
    println("traverseDirectories = " + traverseDirectories)
    println("traverseFiles = " + traverseFiles)
    println("traverseLinks = " + traverseLinks)
  }
  
  val google1 = FileSystems.getDefault().getPath("""D:\Users\Eric\Google Drive""")
  val google2 = FileSystems.getDefault().getPath("""D:\Users\Eric\Google Drive (Original)""")
*/
  //TP(google1, google2)
  
}