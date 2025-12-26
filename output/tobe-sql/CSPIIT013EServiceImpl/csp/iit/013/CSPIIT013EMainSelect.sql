SELECT A.GCC_ORG_CD,
    /*GCC기관코드*/ A.ALL_ORG_NM,
    /*전체기관명*/ F_ORG_INFO(A.BAI_ORG_CD, '1') AS F_ORG_INFO,
    /*감사원기관명*/ A.BAI_ORG_CD,
    /*감사원기관코드*/ A.GCC_CON_HNDL_DVSN_CD,
    /*GCC연계처리구분코드*/ A.PROF_RBPRSN_CRT_YMD AS PROF_RBPRSN_CRT_YMD /* 증명책임자생성일자 */,
    /*생성일*/ A.PROF_RBPRSN_ABL_YMD AS PROF_RBPRSN_ABL_YMD /* 증명책임자폐지일자 */,
    /*폐지일*/ A.BK_ORG_CD,
    /*이전기관코드*/ A.MPP_YN,
    /*매핑여부*/ A.CON_HNDL_DT
FROM TBCSPIIT006M A /*GCC행정코드기본*/ WHERE 1=1 AND A.CON_HNDL_DT &gt;= #strCON_HNDL_DTS# AND A.CON_HNDL_DT &lt;= #strCON_HNDL_DTE# AND A.GCC_ORG_CD &gt;= #strGCC_ORG_CDS# AND A.GCC_ORG_CD &lt;= #strGCC_ORG_CDE# AND A.ALL_ORG_NM LIKE '%'||#strALL_ORG_NM#||'%' AND A.BAI_ORG_CD &gt;= #strBAI_ORG_CDS# AND A.BAI_ORG_CD &lt;= #strBAI_ORG_CDE# AND A.MPP_YN = #strMPP_YN# AND A.GCC_CON_HNDL_DVSN_CD = #strGCC_CON_HNDL_DVSN_CD#