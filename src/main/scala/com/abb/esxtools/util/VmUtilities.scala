package com.abb.esxtools.util

import scala.collection.JavaConversions._
import java.util.ArrayList

import com.vmware.vim25._
import com.vmware.vim25.mo._
import com.vmware.vim25.mo.util._
import com.typesafe.scalalogging._


object VmUtilities {

  // $Regex contains the regular expression of a valid MAC address
  val MacAddressRegex = "00:50:56:([0-9A-Fa-f]){2}:([0-9A-Fa-f]){2}:([0-9A-Fa-f]){2}"

  val DefaultNetworkGroup = "VM Network"

  val DefaultDataCentre = "yvrpd"

  // VM status return codes
  val VmPoweredUp = 0
  val VmSuspended = 1
  val VmPoweredDown = 2
  val VmUnknownState = 3
  val VmNotFound = 4

  // Task return codes
  val Success = 0
  val Failure = 1

  // Default time to wait when shutting down OS before declaring success of failure
  // Time is in minutes.
  val DefaultWaitTime: Long = 5L

  // One minute in milliseconds
  val OneMinute: Long = (60 * 1000)
  
  // Device labels
  val PciControllerLabel = "PCI controller"
  val NetworkAdapterLabel = "Network apdapter"

  // Managed entity labels
  val VirtualMachineEntity = "VirtualMachine"
  val DataCentreEntity = "Datacenter"
  
  /**
   * Validates a MAC address's adherence to specific pattern
   *
   * @param macAddress  the mac address to be evaluated
   */
  def isValidMacAddress(macAddress: String): Boolean = {
    macAddress.matches(VmUtilities.MacAddressRegex)
  }
}

class VmUtilities extends LazyLogging {

  /**
   * Returns a list of the PCI controller keys associated with a virtual machine
   *
   * @param vm  the virtual machine to retriece the PCI controller keys from
   */
  def getControllerKeys(vm: VirtualMachine): Array[Int] = {

    logger.trace(s"Begin getControllerKeys")

    val controllerKeys = new ArrayList[Integer]()
    val vmConfig = vm.getConfig.asInstanceOf[VirtualMachineConfigInfo]
    val vds = vmConfig.getHardware.getDevice
    
    val pciControllers = for (v <- vds;
        debug = logger.debug(s"${v.getDeviceInfo.getLabel}: ${v.getKey}")
        if v.getDeviceInfo.getLabel.contains(VmUtilities.PciControllerLabel)
      ) yield v.getKey

    logger.trace(s"End getControllerKeys")

    pciControllers
  }

  /**
   * Returns a list of the Network Adapter keys associated with a virtual machine
   *
   * @param vm  the virtual machine to retrieve the network adapter keys from
   */
  def getNetworkAdapterKeys(vm: VirtualMachine): Array[Int] = {

    logger.trace(s"Begin getNetworkAdapterKeys")

    val vmConfig = vm.getConfig.asInstanceOf[VirtualMachineConfigInfo]
    // val adapterKeys = new ArrayList[Integer]()
    val vds = vmConfig.getHardware.getDevice
  
    // for (k <- 0 until vds.length) {
    //   logger.debug(s"key $k = ${vds(k).getKey}")
    //   logger.debug(s"key $k label = ${vds(k).getDeviceInfo.getLabel}")

    //   if (vds(k).getDeviceInfo.getLabel.contains(VmUtilities.NetworkAdapterLabel)) adapterKeys.add(vds(k).getKey)
    // }

    val adapterKeys = for (v <- vds;
      _ = logger.debug(s"${v.getDeviceInfo.getLabel}: ${v.getKey}")

        if v.getDeviceInfo.getLabel.contains(VmUtilities.NetworkAdapterLabel)
      ) yield v.getKey

    logger.trace(s"End getNetworkAdapterKeys")

    adapterKeys
  }

  /**
   * Returns the first network adaptor for a virtual machine
   *
   * @param vm  the virtual machine to retriece the network adaptor from.
   */
  def getFirstNetworkAdapter(vm: VirtualMachine): Option[VirtualDevice] =  {

    logger.trace(s"Begin getFirstNetworkAdapter")

    val vmConfig: VirtualMachineConfigInfo = vm.getConfig.asInstanceOf[VirtualMachineConfigInfo]
    val vds: Array[VirtualDevice] = vmConfig.getHardware.getDevice

    val networkAdapters = for (v <- vds;
        _ = logger.debug(s"${v.getDeviceInfo.getLabel}: ${v.getKey}")
        if v.getDeviceInfo.getLabel.contains(VmUtilities.NetworkAdapterLabel)
      ) yield v

    logger.trace(s"End getFirstNetworkAdapter")

    if (networkAdapters.length == 0) None else Some(networkAdapters(0))

    // for (k <- 0 until vds.length if vds(k).getDeviceInfo.getLabel.contains(VmUtilities.NetworkAdapterLabel)) {
    //   return Some(vds(k))
    // }
  
    // None
  }

  def getVMs(rootFolder: Folder): List[VirtualMachine] =
    new InventoryNavigator(rootFolder).searchManagedEntities(VmUtilities.VirtualMachineEntity) match {
      case null =>
        logger.error("searchManagedEntities returned null")
        List[VirtualMachine]()
      case vms =>
        (vms map(_.asInstanceOf[VirtualMachine])).toList
    }

  def getDataCentres(rootFolder: Folder): List[String] = {

    val datacentres = new InventoryNavigator(rootFolder).searchManagedEntities(VmUtilities.DataCentreEntity)
    val datacentreList = new ArrayList[String]()

    for (i <- 0 until datacentres.length) {
      val dc = datacentres(i).asInstanceOf[Datacenter]
      datacentreList.add(dc.getName)
    }

    datacentreList.toList
  }
}