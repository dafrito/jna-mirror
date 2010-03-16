/* Copyright (c) 2007 Timothy Wall, All Rights Reserved
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package com.sun.jna.platform.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;

/**
 * Netapi32.dll Interface.
 * @author dblock[at]dblock.org
 */
public interface Netapi32 extends W32API {
	Netapi32 INSTANCE = (Netapi32) Native.loadLibrary("Netapi32",
			Netapi32.class, W32APIOptions.UNICODE_OPTIONS);

	/**
	 * Retrieves join status information for the specified computer.
	 * 
	 * @param lpServer
	 *            Specifies the DNS or NetBIOS name of the computer on which to
	 *            call the function.
	 * @param lpNameBuffer
	 *            Receives the NetBIOS name of the domain or workgroup to which
	 *            the computer is joined.
	 * @param BufferType
	 *            Join status of the specified computer.
	 * @return If the function succeeds, the return value is NERR_Success. If
	 *         the function fails, the return value is a system error code.
	 */
	public int NetGetJoinInformation(String lpServer,
			PointerByReference lpNameBuffer, IntByReference BufferType);

	/**
	 * Frees the memory that the NetApiBufferAllocate function allocates.
	 * 
	 * @param buffer
	 * @return If the function succeeds, the return value is NERR_Success. If
	 *         the function fails, the return value is a system error code.
	 */
	public int NetApiBufferFree(Pointer buffer);

	/**
	 * Returns information about each local group account on the specified
	 * server.
	 * 
	 * @param serverName
	 *            Specifies the DNS or NetBIOS name of the remote server on
	 *            which the function is to execute. If this parameter is NULL,
	 *            the local computer is used.
	 * @param level
	 *            Specifies the information level of the data.
	 * @param bufptr
	 *            Pointer to the address of the buffer that receives the
	 *            information structure.
	 * @param prefmaxlen
	 *            Specifies the preferred maximum length of returned data, in
	 *            bytes.
	 * @param entriesread
	 *            Pointer to a value that receives the count of elements
	 *            actually enumerated.
	 * @param totalentries
	 *            Pointer to a value that receives the approximate total number
	 *            of entries that could have been enumerated from the current
	 *            resume position.
	 * @param resume_handle
	 *            Pointer to a value that contains a resume handle that is used
	 *            to continue an existing local group search.
	 * @return If the function succeeds, the return value is NERR_Success.
	 */
	public int NetLocalGroupEnum(String serverName, int level,
			PointerByReference bufptr, int prefmaxlen,
			IntByReference entriesread, IntByReference totalentries,
			IntByReference resume_handle);
	
	/**
	 * Returns the name of the primary domain controller (PDC).
	 * 
	 * @param serverName 
	 * 	Specifies the DNS or NetBIOS name of the remote server on which the function is 
	 * 	to execute. If this parameter is NULL, the local computer is used. 
	 * @param domainName
	 * 	Specifies the name of the domain. 
	 * @param bufptr
	 * 	Receives a string that specifies the server name of the PDC of the domain.
	 * @return 
	 * 	If the function succeeds, the return value is NERR_Success.
	 */
	public int NetGetDCName(String serverName, String domainName, 
			PointerByReference bufptr);

	/**
	 * The NetGroupEnum function retrieves information about each global group 
	 * in the security database, which is the security accounts manager (SAM) database or,
	 * in the case of domain controllers, the Active Directory.
	 * @param servername
	 *  Pointer to a constant string that specifies the DNS or NetBIOS name of the 
	 *  remote server on which the function is to execute. If this parameter is NULL, 
	 *  the local computer is used. 
	 * @param level
	 *  Specifies the information level of the data. 
	 * @param bufptr
	 *  Pointer to the buffer to receive the global group information structure. 
	 *  The format of this data depends on the value of the level parameter. 
	 * @param prefmaxlen
	 *  Specifies the preferred maximum length of the returned data, in bytes. 
	 *  If you specify MAX_PREFERRED_LENGTH, the function allocates the amount of 
	 *  memory required to hold the data. If you specify another value in this 
	 *  parameter, it can restrict the number of bytes that the function returns. 
	 *  If the buffer size is insufficient to hold all entries, the function 
	 *  returns ERROR_MORE_DATA.
	 * @param entriesread
	 *  Pointer to a value that receives the count of elements actually enumerated. 
	 * @param totalentries
	 *  Pointer to a value that receives the total number of entries that could have 
	 *  been enumerated from the current resume position. The total number of entries 
	 *  is only a hint.
	 * @param resume_handle
	 *  Pointer to a variable that contains a resume handle that is used to continue 
	 *  the global group enumeration. The handle should be zero on the first call and 
	 *  left unchanged for subsequent calls. If resume_handle is NULL, no resume handle 
	 *  is stored. 
	 * @return
	 *  If the function succeeds, the return value is NERR_Success.
	 */
	public int NetGroupEnum(String servername, int level, PointerByReference bufptr,
			int prefmaxlen, IntByReference entriesread, IntByReference totalentries,
			IntByReference resume_handle);

	/**
	 * The NetUserEnum function provides information about all user accounts on a server.	 
	 * @param servername
	 *  Pointer to a constant string that specifies the DNS or NetBIOS name of the 
	 *  remote server on which the function is to execute. If this parameter is NULL, 
	 *  the local computer is used. 
	 * @param level
	 *  Specifies the information level of the data.
	 * @param filter
	 *  Specifies a value that filters the account types for enumeration.
	 * @param bufptr
	 *  Pointer to the buffer that receives the data. The format of this data depends 
	 *  on the value of the level parameter. This buffer is allocated by the system and
	 *  must be freed using the NetApiBufferFree function. Note that you must free the
	 *  buffer even if the function fails with ERROR_MORE_DATA. 
	 * @param prefmaxlen
	 *  Specifies the preferred maximum length, in 8-bit bytes of returned data. If you 
	 *  specify MAX_PREFERRED_LENGTH, the function allocates the amount of memory 
	 *  required for the data. If you specify another value in this parameter, it can
	 *  restrict the number of bytes that the function returns. If the buffer size is
	 *  insufficient to hold all entries, the function returns ERROR_MORE_DATA. 
	 * @param entriesread
	 *  Pointer to a value that receives the count of elements actually enumerated. 
	 * @param totalentries
	 *  Pointer to a value that receives the total number of entries that could have 
	 *  been enumerated from the current resume position. Note that applications should 
	 *  consider this value only as a hint.
	 * @param resume_handle
	 *  Pointer to a value that contains a resume handle which is used to continue an 
	 *  existing user search. The handle should be zero on the first call and left 
	 *  unchanged for subsequent calls. If resume_handle is NULL, then no resume 
	 *  handle is stored. 
	 * @return
	 *  If the function succeeds, the return value is NERR_Success.
	 */
	public int NetUserEnum(String servername, int level, int filter, PointerByReference bufptr,
			int prefmaxlen, IntByReference entriesread, IntByReference totalentries,
			IntByReference resume_handle);

	/**
	 * The NetUserGetGroups function retrieves a list of global groups to which a 
	 * specified user belongs.
	 * @param servername
	 *  Pointer to a constant string that specifies the DNS or NetBIOS name of the 
	 *  remote server on which the function is to execute. If this parameter is NULL, 
	 *  the local computer is used. 
	 * @param username
	 *  Pointer to a constant string that specifies the name of the user to search for 
	 *  in each group account. For more information, see the following Remarks section. 
	 * @param level
	 *  Specifies the information level of the data.
	 * @param bufptr
	 *  Pointer to the buffer that receives the data. This buffer is allocated by the 
	 *  system and must be freed using the NetApiBufferFree function. Note that you must 
	 *  free the buffer even if the function fails with ERROR_MORE_DATA. 
	 * @param prefmaxlen
	 *  Specifies the preferred maximum length of returned data, in bytes. If you specify 
	 *  MAX_PREFERRED_LENGTH, the function allocates the amount of memory required for the 
	 *  data. If you specify another value in this parameter, it can restrict the number 
	 *  of bytes that the function returns. If the buffer size is insufficient to hold 
	 *  all entries, the function returns ERROR_MORE_DATA.
	 * @param entriesread
	 *  Pointer to a value that receives the count of elements actually retrieved. 
	 * @param totalentries
	 *  Pointer to a value that receives the total number of entries that could have been retrieved. 
	 * @return
	 *  If the function succeeds, the return value is NERR_Success.
	 */
	public int NetUserGetGroups(String servername, String username, int level,
			PointerByReference bufptr, int prefmaxlen,
			IntByReference entriesread, IntByReference totalentries);
 
	/**
	 * The NetUserGetLocalGroups function retrieves a list of local groups to which a
	 * specified user belongs.
	 * @param servername
	 *  Pointer to a constant string that specifies the DNS or NetBIOS name of the remote 
	 *  server on which the function is to execute. If this parameter is NULL, the local 
	 *  computer is used. 
	 * @param username
	 *  Pointer to a constant string that specifies the name of the user for which to return
	 *  local group membership information. If the string is of the form DomainName\UserName 
	 *  the user name is expected to be found on that domain. If the string is of the form 
	 *  UserName, the user name is expected to be found on the server specified by the 
	 *  servername parameter.
	 * @param level
	 *  Specifies the information level of the data.
	 * @param flags
	 *  Specifies a bitmask of flags. Currently, only the value LG_INCLUDE_INDIRECT is 
	 *  defined. If this bit is set, the function also returns the names of the local 
	 *  groups in which the user is indirectly a member (that is, the user has membership 
	 *  in a global group that is itself a member of one or more local groups). 
	 * @param bufptr
	 *  Pointer to the buffer that receives the data. The format of this data depends on 
	 *  the value of the level parameter. This buffer is allocated by the system and must 
	 *  be freed using the NetApiBufferFree function. Note that you must free the buffer 
	 *  even if the function fails with ERROR_MORE_DATA. 
	 * @param prefmaxlen
	 *  Specifies the preferred maximum length of returned data, in bytes. If you specify 
	 *  MAX_PREFERRED_LENGTH, the function allocates the amount of memory required for the 
	 *  data. If you specify another value in this parameter, it can restrict the number of 
	 *  bytes that the function returns. If the buffer size is insufficient to hold all 
	 *  entries, the function returns ERROR_MORE_DATA. For more information, see Network 
	 *  Management Function Buffers and Network Management Function Buffer Lengths. 
	 * @param entriesread
	 *  Pointer to a value that receives the count of elements actually enumerated.
	 * @param totalentries
	 *  Pointer to a value that receives the total number of entries that could have been enumerated. 
	 * @return
	 *  If the function succeeds, the return value is NERR_Success.
	 */
	public int NetUserGetLocalGroups(String servername, String username, int level,
			int flags, PointerByReference bufptr, int prefmaxlen,
			IntByReference entriesread, IntByReference totalentries);
	
	/**
	 * The NetUserAdd function adds a user account and assigns a password and privilege level.
	 * @param servername
	 *  Pointer to a constant string that specifies the DNS or NetBIOS name of the remote server 
	 *  on which the function is to execute.
	 * @param level
	 *  Specifies the information level of the data.
	 * @param buf
	 *  Pointer to the buffer that specifies the data. The format of this data depends on the 
	 *  value of the level parameter.
	 * @param parm_err
	 *  Pointer to a value that receives the index of the first member of the user information 
	 *  structure that causes ERROR_INVALID_PARAMETER. If this parameter is NULL, the index is 
	 *  not returned on error. 
	 * @return
	 *  If the function succeeds, the return value is NERR_Success.
	 */
	public int NetUserAdd(String servername, int level, 
			Structure buf, IntByReference parm_err);
	
	
	/**
	 * The NetUserDel function deletes a user account from a server.
	 * @param servername
	 *  Pointer to a constant string that specifies the DNS or NetBIOS name of the remote 
	 *  server on which the function is to execute. If this parameter is NULL, the local 
	 *  computer is used. 
	 * @param username
	 *  Pointer to a constant string that specifies the name of the user account to delete. 
	 * @return
	 *  If the function succeeds, the return value is NERR_Success.
	 */
	public int NetUserDel(String servername, String username);		
	
	/**
	 * The NetUserChangePassword function changes a user's password for a specified
	 * network server or domain.
	 * @param domainname
	 *  Pointer to a constant string that specifies the DNS or NetBIOS name of a remote 
	 *  server or domain on which the function is to execute. If this parameter is NULL, 
	 *  the logon domain of the caller is used. 
	 * @param username
	 *  Pointer to a constant string that specifies a user name. The NetUserChangePassword 
	 *  function changes the password for the specified user. If this parameter is NULL, 
	 *  the logon name of the caller is used.
	 * @param oldpassword
	 *  Pointer to a constant string that specifies the user's old password. 
	 * @param newpassword
	 *  Pointer to a constant string that specifies the user's new password. 
	 * @return
	 *  If the function succeeds, the return value is NERR_Success.
	 */
	public int NetUserChangePassword(String domainname, String username, 
			String oldpassword, String newpassword);
}
