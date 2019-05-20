/**
 * Proprietary and Confidential
 * Copyright 1995-2010 iBASEt, Inc.
 * Unpublished-rights reserved under the Copyright Laws of the United States
 * US Government Procurements:
 * Commercial Software licensed with Restricted Rights.
 * Use, reproduction, or disclosure is subject to restrictions set forth in
 * license agreement and purchase contract.
 * iBASEt, Inc. 27442 Portola Parkway, Suite 300, Foothill Ranch, CA 92610
 *
 * Solumina software may be subject to United States Dept of Commerce Export Controls.
 * Contact iBASEt for specific Expert Control Classification information.
 */
package com.ibaset.common.security.context;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class CustomInMemoryDaoImpl implements UserDetailsService
{

	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException
	{

		
		SoluminaUser user = new SoluminaUser(	"SFMFG",
												"E9254DB3667DDE8D6421A540C064411FDF230238",
												true,
												true,
												true,
												true,
												getGrantedAuthorityList()
												);
		
		return user;
	}
	
	public static GrantedAuthority[] getGrantedAuthorityList()
	{
		List list = new ArrayList();
		list.add(new SimpleGrantedAuthority("@DeleteUnownedObjects"));
		list.add(new SimpleGrantedAuthority("@OHMfgInstructions$View"));
		list.add(new SimpleGrantedAuthority("@SetInitialFolderRights"));
		list.add(new SimpleGrantedAuthority("@SoluminaDBA"));
		list.add(new SimpleGrantedAuthority("AUDIT_ORDER_CREATE"));
		list.add(new SimpleGrantedAuthority("AUTHORING"));
		list.add(new SimpleGrantedAuthority("BUYOFF_CUST"));
		list.add(new SimpleGrantedAuthority("BUYOFF_MFG"));
		list.add(new SimpleGrantedAuthority("BUYOFF_MFG2"));
		list.add(new SimpleGrantedAuthority("BUYOFF_OVERRIDE_CUST"));
		list.add(new SimpleGrantedAuthority("BUYOFF_OVERRIDE_DC"));
		list.add(new SimpleGrantedAuthority("BUYOFF_OVERRIDE_MFG"));
		list.add(new SimpleGrantedAuthority("BUYOFF_OVERRIDE_MFG2"));
		list.add(new SimpleGrantedAuthority("BUYOFF_OVERRIDE_QA"));
		list.add(new SimpleGrantedAuthority("BUYOFF_QA"));
		list.add(new SimpleGrantedAuthority("BUYOFF_QA_OVERRIDE"));
		list.add(new SimpleGrantedAuthority("CA_APPROVAL"));
		list.add(new SimpleGrantedAuthority("CA_CREATE"));
		list.add(new SimpleGrantedAuthority("CA_FAC_UPDATE"));
		list.add(new SimpleGrantedAuthority("CA_ITEM_CANCEL"));
		list.add(new SimpleGrantedAuthority("CA_ITEM_INITIATION"));
		list.add(new SimpleGrantedAuthority("CA_ITEM_UPDATE"));
		list.add(new SimpleGrantedAuthority("CA_REQUEST_CANCEL"));
		list.add(new SimpleGrantedAuthority("CA_REQUEST_CREATE"));
		list.add(new SimpleGrantedAuthority("CA_UPDATE"));
		list.add(new SimpleGrantedAuthority("CA_VERIFY"));
		list.add(new SimpleGrantedAuthority("CA_VER_CANCEL"));
		list.add(new SimpleGrantedAuthority("CA_VER_INITIATION"));
		list.add(new SimpleGrantedAuthority("CA_VER_UPDATE"));
		list.add(new SimpleGrantedAuthority("CHANGE_DECISION"));
		list.add(new SimpleGrantedAuthority("COMM_AUTHORING"));
		list.add(new SimpleGrantedAuthority("COMM_TEST"));
		list.add(new SimpleGrantedAuthority("CONFIG_UPDATE"));
		list.add(new SimpleGrantedAuthority("CONFIG_VIEW"));
		list.add(new SimpleGrantedAuthority("CUST_MR"));
		list.add(new SimpleGrantedAuthority("DATCOL_MFG"));
		list.add(new SimpleGrantedAuthority("DISC_INITIATION"));
		list.add(new SimpleGrantedAuthority("DISC_ITEM_CANCEL"));
		list.add(new SimpleGrantedAuthority("DISC_ITEM_CLASS_OVERRIDE"));
		list.add(new SimpleGrantedAuthority("DISC_QA_AUTHORING"));
		list.add(new SimpleGrantedAuthority("DISC_REVIEW"));
		list.add(new SimpleGrantedAuthority("DISPATCH_ALL"));
		list.add(new SimpleGrantedAuthority("DISPATCH_COMM"));
		list.add(new SimpleGrantedAuthority("DISPATCH_CUST"));
		list.add(new SimpleGrantedAuthority("DISPATCH_ENG"));
		list.add(new SimpleGrantedAuthority("DISPATCH_MFG"));
		list.add(new SimpleGrantedAuthority("DISPATCH_PRPLG"));
		list.add(new SimpleGrantedAuthority("DISPATCH_QA"));
		list.add(new SimpleGrantedAuthority("DISPATCH_SUPP"));
		list.add(new SimpleGrantedAuthority("DISP_INITIATION"));
		list.add(new SimpleGrantedAuthority("DISP_QA_AUTHORING"));
		list.add(new SimpleGrantedAuthority("DROP_USER"));
		list.add(new SimpleGrantedAuthority("ENG"));
		list.add(new SimpleGrantedAuthority("ENG_MR"));
		list.add(new SimpleGrantedAuthority("GRANT_ROLE"));
		list.add(new SimpleGrantedAuthority("IE_AUTHORING"));
		list.add(new SimpleGrantedAuthority("INSP_DEF_CREATE"));
		list.add(new SimpleGrantedAuthority("INSP_ITEM_CREATE"));
		list.add(new SimpleGrantedAuthority("INSPPLAN_AUTHORING"));
		list.add(new SimpleGrantedAuthority("ITEM_UPDATE"));
		list.add(new SimpleGrantedAuthority("KV_CONFIG_APPROVE"));
		list.add(new SimpleGrantedAuthority("KV_CONFIG_EDIT"));
		list.add(new SimpleGrantedAuthority("KV_CONFIG_VIEW"));
		list.add(new SimpleGrantedAuthority("LOOKUP_TOOL_UPDATE"));
		list.add(new SimpleGrantedAuthority("MANDATE_CREATE"));
		list.add(new SimpleGrantedAuthority("MBOM_UPDATE"));
		list.add(new SimpleGrantedAuthority("MESSAGE_UPDATE"));
		list.add(new SimpleGrantedAuthority("MES_REPORTS"));
		list.add(new SimpleGrantedAuthority("ME_APPROVAL"));
		list.add(new SimpleGrantedAuthority("MFG_REVISE_SCHEDULE"));
		list.add(new SimpleGrantedAuthority("MFG_UPDATE_ACTUALS"));
		list.add(new SimpleGrantedAuthority("MFG_UPDATE_SCHEDULE"));
		list.add(new SimpleGrantedAuthority("MGMT_REVIEW"));
		list.add(new SimpleGrantedAuthority("ORDER_ALTPART_FROMITEMMASTER"));
		list.add(new SimpleGrantedAuthority("ORDER_ALTPART_UPDATE"));
		list.add(new SimpleGrantedAuthority("ORDER_ALT_INITIATION"));
		list.add(new SimpleGrantedAuthority("ORDER_ASSIGN_CREW"));
		list.add(new SimpleGrantedAuthority("ORDER_AUTHORING"));
		list.add(new SimpleGrantedAuthority("ORDER_AUTHORING2"));
		list.add(new SimpleGrantedAuthority("ORDER_CANCEL"));
		list.add(new SimpleGrantedAuthority("ORDER_COMPLETE_WITH_LIEN"));
		list.add(new SimpleGrantedAuthority("ORDER_CONTINUE_WITH_LIEN"));
		list.add(new SimpleGrantedAuthority("ORDER_CREATE"));
		list.add(new SimpleGrantedAuthority("ORDER_DECISION_UNDO"));
		list.add(new SimpleGrantedAuthority("ORDER_DELETE"));
		list.add(new SimpleGrantedAuthority("ORDER_HOLD"));
		list.add(new SimpleGrantedAuthority("ORDER_IE_AUTHORING"));
		list.add(new SimpleGrantedAuthority("ORDER_NOTE"));
		list.add(new SimpleGrantedAuthority("ORDER_OPER_REOPEN"));
		list.add(new SimpleGrantedAuthority("ORDER_OPER_SKIP"));
		list.add(new SimpleGrantedAuthority("ORDER_PLG_AUTHORING"));
		list.add(new SimpleGrantedAuthority("ORDER_QA_AUTHORING"));
		list.add(new SimpleGrantedAuthority("ORDER_RELEASE"));
		list.add(new SimpleGrantedAuthority("ORDER_SIGNON"));
		list.add(new SimpleGrantedAuthority("ORDER_SPLIT"));
		list.add(new SimpleGrantedAuthority("ORDER_SUPERCEDE"));
		list.add(new SimpleGrantedAuthority("OUTLIER_EDIT"));
		list.add(new SimpleGrantedAuthority("PLAN_CREATE"));
		list.add(new SimpleGrantedAuthority("PLAN_DELETE"));
		list.add(new SimpleGrantedAuthority("PLAN_IE_AUTHORING"));
		list.add(new SimpleGrantedAuthority("PLAN_QA_AUTHORING"));
		list.add(new SimpleGrantedAuthority("PLG_ACCEPTANCE"));
		list.add(new SimpleGrantedAuthority("PLG_APPROVAL"));
		list.add(new SimpleGrantedAuthority("PLG_AUTHORING"));
		list.add(new SimpleGrantedAuthority("PLG_REVIEW"));
		list.add(new SimpleGrantedAuthority("PRODUCTION_HOLD"));
		list.add(new SimpleGrantedAuthority("PWP_EDIT"));
		list.add(new SimpleGrantedAuthority("QA_APPROVAL"));
		list.add(new SimpleGrantedAuthority("QA_AUTHORING"));
		list.add(new SimpleGrantedAuthority("QA_MR"));
		list.add(new SimpleGrantedAuthority("QA_PR"));
		list.add(new SimpleGrantedAuthority("QA_REVIEW"));
		list.add(new SimpleGrantedAuthority("REPORT_BROWSE"));
		list.add(new SimpleGrantedAuthority("REPORT_VIEW"));
		list.add(new SimpleGrantedAuthority("REVOKE_ROLE"));
		list.add(new SimpleGrantedAuthority("RPT_XBAR_CTLS_SET"));
		list.add(new SimpleGrantedAuthority("SERIAL_LOT_CHG"));
		list.add(new SimpleGrantedAuthority("SET_UCHART_CTLS"));
		list.add(new SimpleGrantedAuthority("SFOR_ORDER_SUBJECT_INCLUDE"));
		list.add(new SimpleGrantedAuthority("SFOR_OVERHAUL_ORDER_CREATE"));
		list.add(new SimpleGrantedAuthority("SFOR_OVERHAUL_ORDER_VIEW"));
		list.add(new SimpleGrantedAuthority("SFOR_OVERHAUL_PLAN_CREATE"));
		list.add(new SimpleGrantedAuthority("SFOR_OVERHAUL_PLAN_ME_APPROVAL"));
		list.add(new SimpleGrantedAuthority("SFOR_OVERHAUL_PLAN_QA_APPROVAL"));
		list.add(new SimpleGrantedAuthority("SFOR_OVERHAUL_PLAN_VIEW"));
		list.add(new SimpleGrantedAuthority("SFOR_REPAIR_ORDER_VIEW"));
		list.add(new SimpleGrantedAuthority("SFOR_REPAIR_PLAN_ME_APPROVAL"));
		list.add(new SimpleGrantedAuthority("SFOR_REPAIR_PLAN_QA_APPROVAL"));
		list.add(new SimpleGrantedAuthority("SFOR_REPAIR_PLAN_VIEW"));
		list.add(new SimpleGrantedAuthority("SFOR_SYSADMIN"));
		list.add(new SimpleGrantedAuthority("SFSQA_CHAR_APPROVE"));
		list.add(new SimpleGrantedAuthority("SFSQA_CHAR_CREATE"));
		list.add(new SimpleGrantedAuthority("SFSQA_CHAR_SUPERCEDE"));
		list.add(new SimpleGrantedAuthority("SFSQA_CHAR_SUPPLIER_APPROVE"));
		list.add(new SimpleGrantedAuthority("SFSQA_COTSI_PA_AUTHORING"));
		list.add(new SimpleGrantedAuthority("SFSQA_DISPATCH_ALL"));
		list.add(new SimpleGrantedAuthority("SFSQA_GLOBAL_RULES_CREATE"));
		list.add(new SimpleGrantedAuthority("SFSQA_GLOBAL_RULES_DELETE"));
		list.add(new SimpleGrantedAuthority("SFSQA_GLOBAL_RULES_UPDATE"));
		list.add(new SimpleGrantedAuthority("SFSQA_HOLD_CLOSE"));
		list.add(new SimpleGrantedAuthority("SFSQA_HOLD_UPD"));
		list.add(new SimpleGrantedAuthority("SFSQA_IDP_FORCE_REALLOCATION"));
		list.add(new SimpleGrantedAuthority("SFSQA_IDP_PS_FORCE_COMPLT"));
		list.add(new SimpleGrantedAuthority("SFSQA_IDP_REALLOCATE"));
		list.add(new SimpleGrantedAuthority("SFSQA_INIT_UNIT_CREATE"));
		list.add(new SimpleGrantedAuthority("SFSQA_INPROCESS_SAC_PS"));
		list.add(new SimpleGrantedAuthority("SFSQA_INSP_ORDER_CREATE"));
		list.add(new SimpleGrantedAuthority("SFSQA_INSP_PLAN_APPROVE"));
		list.add(new SimpleGrantedAuthority("SFSQA_INSP_PLAN_CREATE"));
		list.add(new SimpleGrantedAuthority("SFSQA_INSP_TYPE_UPD"));
		list.add(new SimpleGrantedAuthority("SFSQA_NMRR_CHARCODE_CREATE"));
		list.add(new SimpleGrantedAuthority("SFSQA_NMRR_INIT"));
		list.add(new SimpleGrantedAuthority("SFSQA_OVER_INSPECTION"));
		list.add(new SimpleGrantedAuthority("SFSQA_PO_DISPATCH"));
		list.add(new SimpleGrantedAuthority("SFSQA_PO_EDIT"));
		list.add(new SimpleGrantedAuthority("SFSQA_RCV_INSP"));
		list.add(new SimpleGrantedAuthority("SFSQA_RED_DECAL"));
		list.add(new SimpleGrantedAuthority("SFSQA_RED_DECAL_ACCEPT_REJECT"));
		list.add(new SimpleGrantedAuthority("SFSQA_RESP_UNIT_CREATE"));
		list.add(new SimpleGrantedAuthority("SFSQA_SAMPLINGPLANREVCREATE"));
		list.add(new SimpleGrantedAuthority("SFSQA_STAMP_CREATE"));
		list.add(new SimpleGrantedAuthority("SFSQA_SUPPLIER_MASS_CHG"));
		list.add(new SimpleGrantedAuthority("SFSQA_SUPP_OI_UPD"));
		list.add(new SimpleGrantedAuthority("SFSQA_SUPP_RATING_DISPATCH"));
		list.add(new SimpleGrantedAuthority("SFSQA_SYSADMIN"));
		list.add(new SimpleGrantedAuthority("SFSQA_USERSTAMP_CREATE"));
		list.add(new SimpleGrantedAuthority("SLIDE_BROWSE"));
		list.add(new SimpleGrantedAuthority("SLIDE_EDIT"));
		list.add(new SimpleGrantedAuthority("SLIDE_VIEW"));
		list.add(new SimpleGrantedAuthority("STDOPER_AUTHORING"));
		list.add(new SimpleGrantedAuthority("STDOPER_SUPERCEDE"));
		list.add(new SimpleGrantedAuthority("STDTEXT_AUTHORING"));
		list.add(new SimpleGrantedAuthority("SUPP"));
		list.add(new SimpleGrantedAuthority("SUPPLIER_CREATE"));
		list.add(new SimpleGrantedAuthority("SYSADMIN_EDIT"));
		list.add(new SimpleGrantedAuthority("SYSADMIN_VIEW"));
		list.add(new SimpleGrantedAuthority("SYSTEM_INTEGRITY_BROWSE"));
		list.add(new SimpleGrantedAuthority("SYSTEM_INTEGRITY_EDIT"));
		list.add(new SimpleGrantedAuthority("TRAINING"));
		list.add(new SimpleGrantedAuthority("UPDATE_AUDITEE_INFORMATION"));
		list.add(new SimpleGrantedAuthority("UPDT_USER"));
		list.add(new SimpleGrantedAuthority("USER_UPDATE"));
		list.add(new SimpleGrantedAuthority("USER_VIEW"));
		list.add(new SimpleGrantedAuthority("VENDOR"));
		list.add(new SimpleGrantedAuthority("ROLE_USER"));
        list.add(new SimpleGrantedAuthority("DATA_COLLECTION_OVERRIDE"));
        list.add(new SimpleGrantedAuthority("TOOL_UPDATE"));
        list.add(new SimpleGrantedAuthority("TOOL_CALIB_FREQ"));
        list.add(new SimpleGrantedAuthority("INSPECTION_HOLD"));
		return (GrantedAuthority[]) list.toArray(new GrantedAuthority[list.size()]);
		
	}


}
