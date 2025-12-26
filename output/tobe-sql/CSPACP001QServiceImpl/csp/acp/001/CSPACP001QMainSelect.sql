/* csp/acp/001/CSPACP001QMainSelect */ SELECT A.GDS_RQS_SNO,
    /*물품요청순번*/ A.GDS_DVSN_CD,
    /*물품구분번호*/ A.DMND_YMD AS DMND_YMD /* 요청일자 */,
    /*요청일*/ D.ABR_DPT_NM_1 AS ABR_DPT_NM_1,
    /*요청자부서명*/ B.USER_NM AS USER_NM /* 사용자명 */,
    /*요청자명*/ A.RQS_TIT_NM,
    /*요청제목*/ C.USER_NM AS USER_NM /* 사용자명 */,
    /*처리자명*/ A.GPCD_PRCS_YMD AS GPCD_PRCS_YMD /* 처리일자 */,
    /*처리일자*/ A.RQSTR_ENO AS RQSTR_ENO /* 요청자직원전산번호 */,
    /*요청자전산번호*/ A.PRCS_PIC_ENO AS PRCS_PIC_ENO /* 처리담당자직원전산번호 */,
    /*처리자전산번호*/ A.DMND_DEPT_CD AS DMND_DEPT_CD /* 요청부서코드 */,
    /*처리자부서코드*/ A.PRCS_CN AS PRCS_CN /* 처리내용 */
FROM TBCSPACP001M A, TBDCMACM001M B, TBDCMACM001M C, TBDCMACM002M D WHERE A.RQSTR_ENO = B.TRGT_NOP_ENO(+) AND A.PRCS_PIC_ENO = C.TRGT_NOP_ENO(+) AND A.DMND_DEPT_CD = D.SRECS_DEPT_CD(+) AND A.GDS_KBN='1' AND A.DMND_YMD &gt;= #strRQS_DTS# AND A.DMND_YMD &lt;= #strRQS_DTE# AND A.GDS_DVSN_CD = #strGDS_DVSN_CD# AND (D.CNSTN_MBCMT_DEPT_NM LIKE '%'||#strSEARCH_NM#||'%' OR A.RQS_TIT_NM LIKE '%'||#strSEARCH_NM#||'%' OR A.DMND_CN LIKE '%'||#strSEARCH_NM#||'%' OR A.PRCS_CN LIKE '%'||#strSEARCH_NM#||'%') AND B.USER_NM LIKE '%'||#strRQR_NAM#||'%' AND C.USER_NM LIKE '%'||#strHNDL_PEN_NAM#||'%' AND A.PRCS_YMD IS NULL AND A.PRCS_YMD IS NOT NULL ORDER BY TO_NUMBER(A.GDS_RQS_SNO) DESC