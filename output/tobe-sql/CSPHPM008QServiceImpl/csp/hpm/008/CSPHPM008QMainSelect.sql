SELECT A.MTRL_SNO,
    /*자료요청순번*/ A.MTRL_DVSN_CD,
    /*자료요청구분코드*/ A.REG_YMD AS REG_YMD /* 등록일자 */,
    /*등록잉ㄹ*/ C.ABR_DPT_NM_1 AS ABR_DPT_NM_1,
    /*등록부서명*/ B.USER_NM AS USER_NM /* 사용자명 */,
    /*등록자명*/ A.PBC_YR,
    /*발행연도*/ A.PBC_PLAC,
    /*발행처*/ A.ATR,
    /*저자*/ A.RSCH_ASMT_TTL AS RSCH_ASMT_TTL /* 연구과제제목 */,
    /*제목*/ A.REGI_TXT,
    /*상세*/ A.SAVE_YN,
    /*저장여부*/ A.RGTR_ENO AS RGTR_ENO /* 등록자직원전산번호 */
FROM TBCSPHPM008M A, /*E-서고자료기본테이블*/ TBDCMACM001M B, /*사용자테이블*/ TBDCMACM002M C /*부서코드*/ WHERE A.RGTR_ENO = B.TRGT_NOP_ENO(+) AND A.REG_DEPT_CD = C.SRECS_DEPT_CD(+) AND A.MTRL_DVSN_CD = #strMTRL_DVSN_CD# AND (A.PBC_PLAC LIKE '%'||#strSEARCH_NM#||'%' OR A.ATR LIKE '%'||#strSEARCH_NM#||'%' OR A.RSCH_ASMT_TTL LIKE '%'||#strSEARCH_NM#||'%' OR A.RSCH_ASMT_TTL LIKE '%'||#strSEARCH_NM#||'%') AND A.RGTR_ENO = #RGTR_ENO# ORDER BY TO_NUMBER(A.MTRL_SNO) DESC