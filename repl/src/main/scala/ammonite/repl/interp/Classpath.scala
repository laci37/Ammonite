package ammonite.repl.interp
import acyclic.file
import ammonite.ops._
import ammonite.repl.Util
import java.io.File

/**
 * Loads the jars that make up the classpath of the scala-js-fiddle
 * compiler and re-shapes it into the correct structure to satisfy
 * scala-compile and scalajs-tools
 */
object Classpath {
  /**
   * In memory cache of all the jars used in the compiler. This takes up some
   * memory but is better than reaching all over the filesystem every time we
   * want to do something.
   */

  var current = Thread.currentThread().getContextClassLoader
  val files = collection.mutable.Buffer.empty[java.io.File]
  files.appendAll(
    System.getProperty("sun.boot.class.path")
          .split(java.io.File.pathSeparator)
          .map(new java.io.File(_))
  )
  while(current != null){
    current match{
      case t: java.net.URLClassLoader =>
        files.appendAll(t.getURLs.map(u => new java.io.File(u.toURI)))
      case _ =>
    }
    current = current.getParent
  }

  val (jarDeps, dirDeps) = files.toVector.filter(_.exists).partition(
    x => x.isFile && !x.getName.endsWith(".so") && !x.getName.endsWith(".jnilib")
  )

  val hash = {
    def helper(f: Path): Array[Byte] = {
      if(f.isFile){
        Util.md5Hash(read.bytes(f))
      } else {
        val subHashes = ls(f).map(helper)
        Util.combineHashes((Util.md5Hash(f.toString.getBytes) +: subHashes) :_*)
      }
    }
    Util.combineHashes(files.map{ f => helper(Path(f)) } :_*)
  }
}
