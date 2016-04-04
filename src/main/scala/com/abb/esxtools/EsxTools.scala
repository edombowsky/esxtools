package com.abb.esxtools
/**
  * Provides tools to manipulate VM's deployed onto a VMware ESX server.
  *
  * Implements the following commands
  *  - Power up an existing VM
  *  - Power down and existing VM
  *  - Shut down Guest OS
  *  - Destroy an existing VM
  *  - Change MAC address
  *  - Get VM's power state (as an exit code)
  *  - list VM's hosted on an ESX server
  *  - list Datacentre's on an ESX server
  * 
  * @see http://viki/confluence/display/YPRPRJ/Groovy+Project+framework+for+the+VMware+ESX+interface+-+EsxTools#GroovyProjectframeworkfortheVMwareESXinterface-EsxTools-ChangeMACAddress
  * @see https://usatl-s-vss-vc.ventyx.us.abb.com/mob
  *
  */

import java.net.URL

// import com.vmware.vim25.{VirtualMachinePowerState, HostDiskDimensionsChs, VirtualDiskSpec, VirtualMachineConfigSpec, VirtualDeviceConfigSpec, VirtualEthernetCardNetworkBackingInfo, VirtualE1000, VirtualDevice, VirtualDeviceConfigSpecOperation, VirtualDeviceConnectInfo}
// import com.vmware.vim25.mo.{Folder, InventoryNavigator, ServiceInstance, Task, VirtualMachine, VirtualDiskManager, Datacenter}
import com.vmware.vim25._
import com.vmware.vim25.mo._
import com.typesafe.scalalogging._

import com.abb.esxtools.util.VmUtilities


/**
 * Enumeration of commands supported by ESXTools
 */
object EsxCommands extends Enumeration {
  type EsxCommands = Value

  val Nop = Value
  val PowerUp = Value("power_up")
  val PowerDown = Value("power_down")
  val PowerDownOs = Value("power_down_os")
  val Status = Value("status")
  val Destroy = Value("destroy")
  val CopyVirtualDisk = Value("copy_virtual_disk")
  val ChangeMacAddress = Value("change_mac_address")
  val List = Value("list")
  val Vms = Value("vms")
  val DataCentres = Value("datacentres")
}
import EsxCommands._


case class EsxToolsConfig(
  esxServerName: String = "",
  userId: String = "", 
  password: String = "", 
  vmName: String = "",
  powerDown: Boolean = false,
  powerUp: Boolean = false,
  destroy: Boolean = false,
  status: Boolean = false,
  command: Option[EsxCommands] = None,
  subCommand: EsxCommands = EsxCommands.Nop,
  waitTime: Long = VmUtilities.DefaultWaitTime,
  source: String = "",
  destination: String = "",
  macAddress: String = "",
  networkGroup: String = VmUtilities.DefaultNetworkGroup,
  annotation: String = "",
  datacentre: String = VmUtilities.DefaultDataCentre)

case class EsxToolsVm(config: EsxToolsConfig) extends LazyLogging {

  val esxServerURL = s"https://${config.esxServerName}/sdk"

  // try {
    val si: ServiceInstance = new ServiceInstance(new URL(esxServerURL), config.userId, config.password, true)
    // val sc: ServiceContent = si.getServiceContent
    val rf: Folder  = si.getRootFolder
    val vm: VirtualMachine = new InventoryNavigator(rf).searchManagedEntity(VmUtilities.VirtualMachineEntity, config.vmName).asInstanceOf[VirtualMachine]
  // } catch {
  //   case e: java.net.UnknownHostException =>
  //     // when cannot connect to host and getting serviceinstance  
  //     logger.error(s"Unable to connect to host: ${config.esxServerName}")
  //   case e: com.vmware.vim25.InvalidLogin =>
  //     // when cannot connect to host and getting serviceinstance  
  //     logger.error(s"Unable to connect to host: ${config.esxServerName}")
  // }

  // def si: ServiceInstance = serviceInstance
  // def vm: VirtualMachine = virtualMachine
}


object EsxTools extends LazyLogging {

  val parser = new scopt.OptionParser[EsxToolsConfig]("EsxTools") {
    // head("EsxTools", "1.x")
    head(s"${hello.BuildInfo.name} v${hello.BuildInfo.version} is © 2016 by ${hello.BuildInfo.organization}")
    help("help") text("prints this usage text")
    version("version") text("prints version information")
    opt[String]('s', "server")
      .required()
      .action { (x, c) => c.copy(esxServerName = x) }
      .text("server is the url to the ESX host")
    opt[String]('i', "userId")
      .required().
      action { (x, c) => c.copy(userId = x) }
      .text("userId to log into the ESX host")
    opt[String]('p', "password")
      .required()
      .action { (x, c) => c.copy(password = x) }
      .text("password used to into the ESX host")
    opt[String]('v', "vmName")
      .required()
      .action { (x, c) => c.copy(vmName = x) }
      .text("virtual machine name to be operated on")
    cmd("list") action { (_, c) => c.copy(command = Some(EsxCommands.List))
      } children(
        cmd("vms") action { (_, c) => c.copy(subCommand = EsxCommands.Vms) },
        cmd("datacentres") action { (_, c) => c.copy(subCommand = EsxCommands.DataCentres) }
        )
    cmd("power_down_os")
      .action { (_, c) => c.copy(command = Some(EsxCommands.PowerDownOs)) }
      .text("shutdown a guest OS on a virtual machine")
      .children(opt[Int]('w', "wait")
                  .required()
                  .action { (x, c) => c.copy(waitTime = x.toLong) }
                  .text(s"wait time in minutes (must be > 1 minute), default ${VmUtilities.DefaultWaitTime}")
      )
    cmd("copy_virtual_disk")
      .action { (_, c) => c.copy(command = Some(EsxCommands.CopyVirtualDisk)) }
      .text("copy a virtual disk form one location to another")
      .children(opt[String]('s', "src")
                  .required()
                  .action { (x, c) => c.copy(source = x) }
                  .text("source virtual disk"),
                opt[String]('d', "dest")
                  .required()
                  .action { (x, c) => c.copy(destination = x) }
                  .text("destination virtual disk"),
                opt[String]('c', "datacentre")
                  .action { (x, c) => c.copy(datacentre = x) }
                  .text(s"datacentre where virtual disks reside, default value ${VmUtilities.DefaultDataCentre}")
      )
    cmd("status")
      .action { (_, c) => c.copy(command = Some(EsxCommands.Status)) }
      .text("get status of a virtual machine")
    cmd("power_down")
      .action { (_, c) => c.copy(command = Some(EsxCommands.PowerDown))}
      .text("power down a virtual machine")
    cmd("power_up")
      .action { (_, c) => c.copy(command = Some(EsxCommands.PowerUp)) }
      .text("power up a virtual machine")
    cmd("destroy")
      .action { (_, c) => c.copy(command = Some(EsxCommands.Destroy)) }
      .text("destroy a virtual machine")
    cmd("change_mac_address")
      .action { (_, c) => c.copy(command = Some(EsxCommands.ChangeMacAddress)) }
      .text("change mac address of a virtual machine")
      .children(opt[String]('m', "macaddr")
                  .required()
                  .action { (x, c) => c.copy(macAddress = x) }
                  .text("mac address"),
                opt[String]('n', "networkgrp")
                  .action { (x, c) => c.copy(networkGroup = x) }
                  .text(s"network group, default is ${VmUtilities.DefaultNetworkGroup}"),
                opt[String]('a', "annotation")
                  .required()
                  .action { (x, c) => c.copy(annotation = x) }
                  .text("annotation to give to a virtual machine")
      )
    checkConfig { c =>
      c.command.getOrElse(EsxCommands.Nop) match {
        case EsxCommands.ChangeMacAddress => 
          if (VmUtilities.isValidMacAddress(c.macAddress)) success
          else failure(s"The mac address [${c.macAddress}] does not match the format ${VmUtilities.MacAddressRegex}") 
        case EsxCommands.PowerDownOs =>
          if (c.waitTime >= 1) success
          else failure("wait time cannot be less than 1 minute") 
        case EsxCommands.Nop => failure("A command is required") 
        case _ => success
      }
    }
  }

  /** 
    * Parse the command line and perform the specified doAction
    *
    * @param args  the command line arguments
    * @return 0 for success, positive non-zero for failure
    */
  def doAction(args: Array[String]): Int = {
    
    parser.parse(args, EsxToolsConfig()) match {
      case Some(config) =>
        logger.info(raw"""
          Starting esxTools with following parameters:
          server    : ${config.esxServerName}
          userId    : ${config.userId}
          vmName    : ${config.vmName}""")

        val esxToolsVm = EsxToolsVm(config)

        if (esxToolsVm.vm == null) {
          logger.error(s"${config.vmName} was not found")
          esxToolsVm.si.getServerConnection.logout()
          VmUtilities.VmNotFound
        } else {
          config.command.getOrElse(EsxCommands.Nop) match {
            case EsxCommands.Status => status(esxToolsVm)
            case EsxCommands.PowerUp => powerUp(esxToolsVm)
            case EsxCommands.PowerDown => powerDown(esxToolsVm)
            case EsxCommands.CopyVirtualDisk => copyVirtualDisk(esxToolsVm)
            case EsxCommands.ChangeMacAddress => changeMacAddress(esxToolsVm)
            case EsxCommands.Destroy => destroy(esxToolsVm)
            case EsxCommands.PowerDownOs => powerDownOs(esxToolsVm)
            case EsxCommands.List =>
              config.subCommand match {
                case EsxCommands.Vms => listVms(esxToolsVm)
                case EsxCommands.DataCentres => listDataCentres(esxToolsVm)
                case _ => { println(s"Command [${config.command}:${config.subCommand}] not yet implemented..."); 1}
              }
            case _ => { println(s"Command [${config.command}] not yet implemented..."); 1}
          }
        }

      case None =>
        // Arguments are bad, error message will have been displayed
        VmUtilities.Failure
      }

  }
 
  def printVm(vm: VirtualMachine) = logger.info(s"${vm.getName}:  ${vm.getRuntime.getPowerState}")

  def listVms(esxToolsVm: EsxToolsVm): Int = { 

    val vmu: VmUtilities = new VmUtilities

    vmu.getVMs(esxToolsVm.rf).foreach( printVm )
    0
  }

  def listDataCentres(esxToolsVm: EsxToolsVm): Int = { 

    val vmu: VmUtilities = new VmUtilities

    vmu.getDataCentres(esxToolsVm.rf).foreach( logger.info(_) )
    0
  }

  // sbt 'run change_mac_address --macaddr 00:50:56:03:03:03 --annotation "Test vm" -s usatl-s-vss-vc.ventyx.us.abb.com -i jenkins1 -p JenkinsOnly1! -v usatl-s-ssvm213'
  /**
   * Changes the MAC address of a Virtual Machine located on an ESX server.
   *
   * The coding of this function was quite difficult to figure out reading
   * through the Vsphere API but with the help of [[http://communities.vmware.com/community/vmtn/server/vsphere/automationtools/onyx?view=overview Project Obyx]]
   * and the excellent VMware Partner Exchange Feb 2010. [[http://communities.vmware.com/thread/261604 Getting Stoned with Project Onyx]]
   * presented by Carter Shanklin it became a bit easier.
   *
   * The method presented in the above presentation was followed and the Onyx
   * GUI produced the following PowerShell code which was then translated into Scala
   *
   * {{{
   * $spec = New-Object VMware.Vim.VirtualMachineConfigSpec
   * $spec.changeVersion = "2012-06-28T01:05:45.905219Z"
   * $spec.deviceChange = New-Object VMware.Vim.VirtualDeviceConfigSpec[] (1)
   * $spec.deviceChange[0] = New-Object VMware.Vim.VirtualDeviceConfigSpec
   * $spec.deviceChange[0].operation = "edit"
   * $spec.deviceChange[0].device = New-Object VMware.Vim.VirtualE1000
   * $spec.deviceChange[0].device.key = 4000
   * $spec.deviceChange[0].device.deviceInfo = New-Object VMware.Vim.Description
   * $spec.deviceChange[0].device.deviceInfo.label = "Network adapter 1"
   * $spec.deviceChange[0].device.deviceInfo.summary = "VM Network"
   * $spec.deviceChange[0].device.backing = New-Object VMware.Vim.VirtualEthernetCardNetworkBackingInfo
   * $spec.deviceChange[0].device.backing.deviceName = "VM Network"
   * $spec.deviceChange[0].device.backing.useAutoDetect = $false
   * $spec.deviceChange[0].device.connectable = New-Object VMware.Vim.VirtualDeviceConnectInfo
   * $spec.deviceChange[0].device.connectable.startConnected = $true
   * $spec.deviceChange[0].device.connectable.allowGuestControl = $true
   * $spec.deviceChange[0].device.connectable.connected = $false
   * $spec.deviceChange[0].device.connectable.status = "untried"
   * $spec.deviceChange[0].device.controllerKey = 100
   * $spec.deviceChange[0].device.unitNumber = 7
   * $spec.deviceChange[0].device.addressType = "manual"
   * $spec.deviceChange[0].device.macAddress = "00:50:56:01:01:01"
   * $spec.deviceChange[0].device.wakeOnLanEnabled = $false
   * 
   * $_this = Get-View -Id 'VirtualMachine-2816'
   * $_this.ReconfigVM_Task($spec)
   * }}}
   *
   * @param macAddress  the new mac address for the virtual machine
   */
  def changeMacAddress(esxToolsVm: EsxToolsVm): Int = { 
    val vmName = esxToolsVm.config.vmName
    val src = esxToolsVm.config.source
    val dest = esxToolsVm.config.destination
    val networkGroup = esxToolsVm.config.networkGroup
    val vmu: VmUtilities = new VmUtilities

    logger.trace(s"Begin changing mac address on vm: ${vmName}")

    try {
      if (esxToolsVm.vm.getRuntime.getPowerState != VirtualMachinePowerState.poweredOff) {
        val spec: VirtualMachineConfigSpec = new VirtualMachineConfigSpec

        val deviceChange: Array[VirtualDeviceConfigSpec] = new Array[VirtualDeviceConfigSpec](1)
        deviceChange(0) = new VirtualDeviceConfigSpec()
        deviceChange(0).setOperation(VirtualDeviceConfigSpecOperation.edit)

        val device: VirtualE1000 = new VirtualE1000
        val networkAdapter: Option[VirtualDevice] = vmu.getFirstNetworkAdapter(esxToolsVm.vm)

        networkAdapter match {
          case Some(networkAdapter) => 
            logger.debug(s"Network adapter key = ${networkAdapter.getKey}")
            // device.setKey(networkAdapter.getKey())   un-commment this when sure things are ready to really be executed

            val backing: VirtualEthernetCardNetworkBackingInfo = new VirtualEthernetCardNetworkBackingInfo

            logger.debug(s"Network group = $networkGroup")

            backing.setDeviceName(networkGroup)
            backing.setUseAutoDetect(false)

            val connectable: VirtualDeviceConnectInfo  = new VirtualDeviceConnectInfo
            connectable.setStartConnected(true)
            connectable.setAllowGuestControl(true)
            connectable.setConnected(false)
            connectable.setStatus("untried")

            val keys: Array[Int] = vmu.getControllerKeys(esxToolsVm.vm)
            logger.debug(s"Controller key = ${keys(0)}")
            device.setControllerKey(keys(0))
            logger.debug(s"Unit Number = ${networkAdapter.getUnitNumber}")
            device.setUnitNumber(networkAdapter.getUnitNumber)

            device.setAddressType("manual")

            device.setMacAddress(esxToolsVm.config.macAddress)
            device.setWakeOnLanEnabled(false)

            device.setConnectable(connectable)
            device.setBacking(backing)
            deviceChange(0).setDevice(device)
            spec.setDeviceChange(deviceChange)
            spec.setAnnotation(esxToolsVm.config.annotation)
            logger.debug(s"Annotation = ${esxToolsVm.config.annotation}")

            // un-comment when ready for execution
            // if (esxToolsVm.vm.reconfigVM_Task(spec).waitForTask == Task.SUCCESS) {
            //   logger.info(s"Changed MAC address for esxToolsVm.vmName to ${esxToolsVm.config.macAddress}")
            //   0
            // } else {
            //   logger.error(s"Failed to change MAC address of $vmName")
            //   1
            // }
            VmUtilities.Success
          case None => 
            logger.error("Unable to get the network adapter")
            VmUtilities.Failure
        }
      } else {
        logger.error(s"vm: $vmName must be powerd off to make changes to MAC address.")
        VmUtilities.Failure
      }
      } finally {
        logger.trace(s"End changing mac address on vm: ${vmName} and close server connection")
        esxToolsVm.si.getServerConnection.logout()
      }
  }

  // sbt 'run copy_virtual_disk --src "[nfsdsdevvm] usatl-s-ssvm214/usatl-s-ssvm214.vmdk" --dest "[nfsdsdevvm] usatl-s-ssvm214/usatl-s-ssvm214_2.vmdk" -s usatl-s-vss-vc.ventyx.us.abb.com -i <username> -p <password> -v usatl-s-ssvm33'
  /**
   * Make a copy of a vitual disk on a virtual machine
   *
   * @param      esxToolsVm  virtual machine data structure
   *
   * @return     1 on error, otherwise 0
   */
  def copyVirtualDisk(esxToolsVm: EsxToolsVm): Int = { 

    val vmName = esxToolsVm.config.vmName
    val src = esxToolsVm.config.source
    val dest = esxToolsVm.config.destination
    val datacentre = esxToolsVm.config.datacentre

    logger.trace(s"Begin virtual disk copy from '$src' to '$dest' on vm: ${vmName}")

    val vdMgr: VirtualDiskManager = esxToolsVm.si.getVirtualDiskManager()

    if (vdMgr == null) {
      println("VirtualDiskManager not available aborting virtual disk copy")
      esxToolsVm.si.getServerConnection().logout()
      1
    } else {
      try {
        val dc: Datacenter = new InventoryNavigator(esxToolsVm.rf).searchManagedEntity(VmUtilities.DataCentreEntity, datacentre).asInstanceOf[Datacenter]

        if (dc == null) {
          logger.error("Datacentre not found")
          VmUtilities.Failure
        } else {
          // val hddc: HostDiskDimensionsChs = vdMgr.queryVirtualDiskGeometry(src, dc)
          // logger.debug(s"Cylinder: ${hddc.getCylinder}")
          // logger.debug(s"Head: ${hddc.getHead}")
          // logger.debug(s"Sector: ${hddc.getSector}")
      
          if (vdMgr.copyVirtualDisk_Task(src, dc, dest, dc, null, false).waitForTask == Task.SUCCESS) {
            logger.info(s"virtual disk $src copied to $dest on vm: ${vmName}")
            VmUtilities.Success
          } else {
            logger.error(s"Error copying $src to $dest on vm: ${vmName}")
            VmUtilities.Failure
          }
        }
      } catch {
        case e: com.vmware.vim25.RestrictedVersion =>
          logger.error("This is a restricted ESx server, unable to perform power off")
          VmUtilities.Failure
        case e: Exception =>
          logger.error("Unexpected exception")
          VmUtilities.Failure
      } finally {
        logger.trace(s"End copy of virtual disk on vm: ${vmName} and close server connection")
        esxToolsVm.si.getServerConnection.logout()
      }
    }
  }

  /**
   * Power down a virtual machine
   *
   * @param      esxToolsVm  virtual machine data structure
   *
   * @return     1 on error, otherwise 0
   */
  def powerDown(esxToolsVm: EsxToolsVm): Int = { 

    val vmName: String = esxToolsVm.config.vmName

    logger.trace(s"Begin power down of vm: ${vmName}")

    try {
      if (esxToolsVm.vm.getRuntime.getPowerState != VirtualMachinePowerState.poweredOff) {
        try {
          if (esxToolsVm.vm.powerOffVM_Task.waitForTask == Task.SUCCESS) {
            logger.info(s"${vmName} was powered off")
            VmUtilities.Success
          } else {
            logger.error(s"Failed to power down vm: ${vmName}")
            VmUtilities.Failure
          }
        } catch {
          case e: com.vmware.vim25.RestrictedVersion =>
            logger.error("This is a restricted ESx server, unable to perform power off")
            VmUtilities.Failure
          case e: Exception =>
            logger.error("Unexpected exception", e)
            VmUtilities.Failure
        }
      } else {
        logger.info(s"${vmName} is already powered off")
        VmUtilities.Failure
      }
    } finally {
      logger.trace(s"End power down of vm: ${vmName} and close server connection")
      esxToolsVm.si.getServerConnection.logout()
    }
  }

  /**
   * Power up a virtual machine
   *
   * @param      esxToolsVm  virtual machine data structure
   *
   * @return     1 on error, otherwise 0
   */
  def powerUp(esxToolsVm: EsxToolsVm): Int = {

    val vmName: String = esxToolsVm.config.vmName

    logger.trace(s"Begin power up of vm: ${vmName}")

    try {
      if (esxToolsVm.vm.getRuntime.getPowerState != VirtualMachinePowerState.poweredOn) {
        try {
          if (esxToolsVm.vm.powerOnVM_Task(null).waitForTask == Task.SUCCESS) {
            logger.info(s"${vmName} was powered on")
            VmUtilities.Success
          } else {
            logger.error(s"Failed to power on vm: ${vmName}")
            VmUtilities.Failure
          }
        } catch {
          case e: com.vmware.vim25.RestrictedVersion =>
            logger.error("This is a restricted ESx server, unable to perform power on")
            VmUtilities.Failure
          case e: Exception =>
            logger.error("Unexpected exception", e)
            VmUtilities.Failure
        }
      } else {
        logger.info(s"${vmName} is already powered on")
        VmUtilities.Failure
      }
    } finally {
      logger.trace(s"End power up of vm: ${vmName} and close server connection")
      esxToolsVm.si.getServerConnection.logout()
    }
  }

  /**
   * Destroy a virtual machine
   *
   * @param      esxToolsVm  virtual machine data structure
   *
   * @return     1 on error, otherwise 0
   */
  def destroy(esxToolsVm: EsxToolsVm): Int = {

    val vmName: String = esxToolsVm.config.vmName

    logger.trace(s"Begin destruction of vm: ${vmName}")

    try {
      if (esxToolsVm.vm.destroy_Task.waitForTask == Task.SUCCESS) {
        logger.info(s"${vmName} was destroyed")
        VmUtilities.Success
      } else {
        logger.error(s"Failed to destroy vm: ${vmName}")
        VmUtilities.Failure
      }
    } catch {
      case e: com.vmware.vim25.RestrictedVersion =>
        logger.error("This is a restricted ESx server, unable to destroy virtual machines")
        VmUtilities.Failure
      case e: Exception =>
        logger.error("Unexpected exception", e)
        VmUtilities.Failure
    } finally {
      logger.trace(s"End power up of vm: ${vmName} and close server connection")
      esxToolsVm.si.getServerConnection.logout()
    }
  }

  /**
   * Power down the OS on a virtual machine
   *
   * @param      esxToolsVm  virtual machine data structure
   *
   * @return     1 on error, otherwise 0
   */
  def powerDownOs(esxToolsVm: EsxToolsVm): Int = { 

    val vmName: String = esxToolsVm.config.vmName

    logger.trace(s"Begin power down OS on vm: ${vmName}")

    try {
      try {
        esxToolsVm.vm.shutdownGuest
        val waitTime = esxToolsVm.config.waitTime

        var nLoops = 0L
        for (i <- 0.toLong to waitTime; if (esxToolsVm.vm.getRuntime.getPowerState != VirtualMachinePowerState.poweredOff)) {
          Thread sleep VmUtilities.OneMinute
          nLoops = i
        }

        if (nLoops < waitTime) {
          logger.info(s"vm: $vmName OS shut down after $nLoops minutes.")
          VmUtilities.Success          
        } else {
          logger.info(s"Failed to shut down OS on vm: $vmName in ${waitTime} minutes")
          VmUtilities.Failure
        }

      } catch  {
        case e: com.vmware.vim25.ToolsUnavailable =>
          logger.error(s"VMware tools are not running, unable to power down vm: ${vmName}")
          VmUtilities.Failure
        case e: com.vmware.vim25.TaskInProgress =>
          logger.error("Another changing OS state operation is probably in progress, trying to shut down OS")
          VmUtilities.Failure
        case e: com.vmware.vim25.RestrictedVersion =>
          logger.error(s"This is a restricted ESx server, unable to perform shut down OS on vm: ${vmName}")
          VmUtilities.Failure
        case e: com.vmware.vim25.InvalidPowerState =>
          logger.info(s"${vmName} is already powered off")
          VmUtilities.Failure
        case e: Exception =>
          logger.error("Unexpected exception", e)
          VmUtilities.Failure
      }

    } finally {
      logger.trace(s"End power down OS on vm: ${vmName} and close server connection")
      esxToolsVm.si.getServerConnection.logout()
    }
  }

  /**
   * Get the status of a virtual machine
   *
   * @param      esxToolsVm  virtual machine data structure
   *
   * @return     0=up, 1=suspended, 2=down, 4=unknown, 5=bad VM name
   */
  def status(esxToolsVm: EsxToolsVm): Int = { //config: EsxToolsConfig): Int = {

    val vmName: String = esxToolsVm.config.vmName

    logger.trace(s"Begin get status of vm: ${vmName}")

    try {
      esxToolsVm.vm.getRuntime.getPowerState match {
        case VirtualMachinePowerState.poweredOn => 
          logger.info(s"${vmName} status is ${VirtualMachinePowerState.poweredOn}")
          VmUtilities.VmPoweredUp
        case VirtualMachinePowerState.suspended => 
          logger.info(s"${vmName} status is ${VirtualMachinePowerState.suspended}")
          VmUtilities.VmSuspended
        case VirtualMachinePowerState.poweredOff => 
          logger.info(s"${vmName} status is ${VirtualMachinePowerState.poweredOff}")
          VmUtilities.VmPoweredDown
      }
    } catch {
      case e: java.lang.NullPointerException =>
        logger.error(s"${vmName} was not found")
        VmUtilities.VmNotFound
      case e: Exception =>
        logger.error(s"${vmName} unknown state", e)
        VmUtilities.VmUnknownState
    } finally {
      logger.trace(s"End get status for vm: ${vmName} and close server connection")
      esxToolsVm.si.getServerConnection.logout()
    }
  }

  def main(args: Array[String]): Unit =  {
    val returnCode = doAction(args)

    sys.exit(returnCode)
  }
}
