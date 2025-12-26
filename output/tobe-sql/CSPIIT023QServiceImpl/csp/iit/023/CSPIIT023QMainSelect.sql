SELECT A.DATA_DMND_SQNO /* 자료요청순번 */,
    /*자료요청순번*/ A.MTRL_RQS_DVSN_CD,
    /*자료요청구분코드*/ A.DMND_YMD AS DMND_YMD /* 요청일자 */,
    /*요청일*/ D.ABR_DPT_NM_1 AS ABR_DPT_NM_1,
    /*요청자부서명*/ B.USER_NM AS USER_NM /* 사용자명 */,
    /*요청자명*/ A.RQS_TIT_NM,
    /*요청제목*/ C.USER_NM AS USER_NM /* 사용자명 */,
    /*처리담당자명*/ A.GPCD_PRCS_YMD AS GPCD_PRCS_YMD /* 처리일자 */,
    /*처리일자*/ A.RQSTR_ENO AS RQSTR_ENO /* 요청자직원전산번호 */,
    /*요청자전산번호*/ A.PRCS_PIC_ENO AS PRCS_PIC_ENO /* 처리담당자직원전산번호 */,
    /*처리자전산번호*/ A.DMND_DEPT_CD AS DMND_DEPT_CD /* 요청부서코드 */,
    /*요청부서코드*/ F_CODE_NM('1000796',A.DGE1_MNU_DVSN_CD) AS F_CODE_NM /* 1차메뉴구분코드 */
FROM TBCSPIIT002M A, /*PC장비관리테이블*/ TBDCMACM001M B, /*사용자테이블(요청자)*/ TBDCMACM001M C, /*사용자테이블(처리자)*/ TBDCMACM002M D, /*부서코드(요청자)*/ TBDCMACM002M E /*부서코드(처리자)*/ WHERE A.RQSTR_ENO = B.TRGT_NOP_ENO(+) AND A.PRCS_PIC_ENO = C.TRGT_NOP_ENO(+) AND A.DMND_DEPT_CD = D.SRECS_DEPT_CD(+) AND C.SRECS_DEPT_CD = E.SRECS_DEPT_CD(+) AND A.COPUT_BRD_DVSN_CD = '03' /*게시판구분코드 - 전산개선요청*/ AND A.DMND_YMD &gt;= #strRQS_DTS# AND A.DMND_YMD &lt;= #strRQS_DTE# AND A.MTRL_RQS_DVSN_CD = #strMTRL_RQS_DVSN_CD# AND (D.CNSTN_MBCMT_DEPT_NM LIKE '%'||#strSEARCH_NM#||'%' OR E.CNSTN_MBCMT_DEPT_NM LIKE '%'||#strSEARCH_NM#||'%' OR A.RQS_TIT_NM LIKE '%'||#strSEARCH_NM#||'%' OR A.DMND_CN LIKE '%'||#strSEARCH_NM#||'%' OR A.PRCS_CN LIKE '%'||#strSEARCH_NM#||'%') AND B.USER_NM LIKE '%'||#strRQR_NAM#||'%' AND C.USER_NM LIKE '%'||#strHNDL_PEN_NAM#||'%' AND ( A.RLS_YN = 'Y' OR A.RQSTR_ENO = #RQSTR_ENO#) AND A.PRCS_YMD IS NULL AND A.PRCS_YMD IS NOT NULL AND A.DGE1_MNU_DVSN_CD = #strDGE1_MNU_DVSN_CD# ORDER BY TO_NUMBER(A.DATA_DMND_SQNO) DESC