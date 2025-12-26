select B.ENFC_DEPT_CD /* 시행부서코드 */,
    /*부서코드*/ B.DOC_SNDNG_NO AS DOC_SNDNG_NO /* 문서발송번호 */,
    /*시행문서번호*/ B.DSPS_RQT_IDNO AS DSPS_RQT_IDNO /* 처분요구식별번호 */,
    /*처분요구번호*/ B.ENFC_YMD AS ENFC_YMD /* 시행일자 */,
    /*시행일자*/ F_ORG_INFO(B.CMO_INST_CD, '1') AS F_ORG_INFO,
    /*소관청명*/ F_ORG_INFO(B.REL_INST_CD, '1') AS F_ORG_INFO,
    /*처분요구기관명*/ F_CODE_NM('1000002', A.DSPS_RQT_KDCD ) AS F_CODE_NM,
    /*처분요구명*/ A.DSPS_RQT_NM AS DSPS_RQT_NM /* 처분요구명 */
from TB_BADEAR001M a, TB_BADEAR002D b where A.ADT_YR = B.ADT_YR and A.ADT_NO = B.ADT_NO and A.DSPS_RQT_SQNO = B.DSPS_RQT_SQNO and B.ENFC_YMD between #strStartFromDt#||'01' and #strStartToDt#||'31' and B.CFMTN_STTS_SECD != '0' and A.DSPS_RQT_KDCD <= '730' and SUBSTR(B.VIOL_TPCD, 1, 1) = '2' and SUBSTR(B.CMO_INST_CD, 1, 3) != SUBSTR(B.REL_INST_CD, 1, 3)