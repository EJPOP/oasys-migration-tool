SELECT A.LINK_YR /* 연계년도 */,
    A.CRS_CD,
    A.CRS_CDN /* 과정기수 */,
    A.TANE_ID,
    NVL(A.GRNDS_EXMN_AEX_SQNO,'') AS GRNDS_EXMN_AEX_SQNO /* 현장조사수용비순번 */,
    A.INFO_RLPR_NM /* 정보관계자명 */,
    A.RAL_BLT_NM,
    A.NPL_BLT_NM,
    A.JBGD_NM /* 직급명 */,
    TRIM(REPLACE(A.SLCT_CVLPR_MPNO, '-')) AS SLCT_CVLPR_MPNO /* 청탁민원인휴대전화번호 */,
    A.TRGT_NOP_ENO /* 대상인원직원전산번호 */
FROM TBCSPSAE306D A WHERE A.LINK_YR = #LINK_YR# AND A.CRS_CD = #strCRS_CD# AND A.CRS_CDN = #CRS_CDN# AND A.SIGN_STATUS = '3' ORDER BY FRST_REG_DT