package net.kolotyluk.scala.files

import java.nio.file.FileSystems
import java.nio.file.Path

/**
 * @author Eric Kolotyluk
 */
object App {
  
  def foo(x : Array[String]) = x.foldLeft("")((a,b) => a + b)
  
  def main(args : Array[String]) {
    println( "Hello World!" )
    println("concat arguments = " + foo(args))
    
//    val paths = TraversablePaths(List(FileSystems.getDefault().getPath("c:\\temp")), false, true, true)
//    println
//    paths.foreach(println)
    
//    java.nio.file.Files.delete(FileSystems.getDefault().getPath("""D:\Users\Eric\Google Drive (New)\Music\Downloaded\Michael Jackson - Discography (1967-2009) [FLAC]\The Jacksons\1976 - The Jacksons {EPC 88697304722CD1}\09 - Dreamer.flac"""))
    
//    val files = paths.filter(_.toFile().isFile())
//    println
//    files.foreach(println)
    
//    val lengths = paths.groupBy((path: Path) => path.toFile().length())
    
//    println
//    println(lengths)
//    
//    val s = Set.empty
//    s.count(f => true)
        
    def delete(path : Path) {
      try {
        println("deleting " + path)
        java.nio.file.Files.delete(path)
      } catch {
        case exception: Exception => System.err.println(exception)
      }
    }
    
    val google1 = FileSystems.getDefault().getPath("""D:\Users\Eric\Google Drive""")
    val google2 = FileSystems.getDefault().getPath("""D:\Users\Eric\Google Drive (Maintenance)""")
    
    val duplicates = DuplicateFinder(google1, google2)().findFiles
    
    println("deleting duplicate files")
    duplicates.foreach(_.filter(!_.startsWith(google1)).foreach(delete))
  }

}
