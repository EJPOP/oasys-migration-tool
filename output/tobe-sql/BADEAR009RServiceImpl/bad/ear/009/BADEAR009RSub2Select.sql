select RELPSN.ADT_YR /* 감사년도 */,
    RELPSN.ADT_NO /* 감사번호 */,
    RELPSN.DSPS_RQT_SQNO /* 처분요구순번 */,
    RELPSN.RLPR_NM /* 관계자명 */,
    /*관계자성명*/ RELPSN.DSPS_RQT_AMT AS DSPS_RQT_AMT /* 처분요구금액 */,
    /*관계자금액*/ F_CODE_NM('1000057', RELPSN.OCPT_SECD) AS OCPT_NM /* 직종명 */,
    /*직종코드명*/ F_CODE_NM('1000058', RELPSN.JBGD_SECD) AS JBGD_NM /* 직급명 */,
    /*관련자직급명*/ F_CODE_NM('1000011', RELPSN.ACTN_RQT_KDCD) AS F_CODE_NM /* 조치요구종류명 */
from TB_BADEAR007M relpsn WHERE 1=1 AND RELPSN.ADT_YR = #CVLCPT_ADT_YR# /*인자값 1*/ AND RELPSN.ADT_NO = #ADT_NO# /*인자값 2*/ AND RELPSN.DSPS_RQT_SQNO = #DSPS_RQT_SQNO# /*인자값 3*/