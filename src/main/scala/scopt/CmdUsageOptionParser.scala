package scopt

/**
  * Extends scopt OptionParser, adds the ability to print usage for individual
  * sub-commands.
  */
class CmdUsageOptionParser[C](programName: String) extends OptionParser[C](programName) {

  private[this] var usageOverride: Option[String] = None

  def setUsage(u: String) = {
    this.usageOverride = Some(u)
  }

  override def usage: String = this.usageOverride.getOrElse(super.usage)

  def renderTwoColumnsUsage(cmd: String): Option[String] = {
    import OptionDef._
    val xs = optionsForRender(cmd)
    xs match {
      case Nil => None
      case _ =>
        val descriptions = {
          val col1Len = math.min(column1MaxLength, (xs map {_.usageColumn1.length + WW.length}).max)
          xs map {_.usageTwoColumn(col1Len)}
        }
        Some((if (header == "") "" else header + NL) +
          //"Usage: " + usageExample + NLNL +
          descriptions.mkString(NL))
    }
  }

  override private[scopt] def commandExample(cmd: Option[OptionDef[_, C]]) = {
    "doto " + super.commandExample(cmd)
  }

  def optionsForRender(cmd: String): List[OptionDef[_, C]] = {
    val cmdOpt = options find { o => o.kind == Cmd && o.name == cmd }
    cmdOpt map { cmd =>
      List(cmd) ++ (options filter { _.getParentId == Some(cmd.id) })
    } getOrElse {
      Nil
    }
  }
}

