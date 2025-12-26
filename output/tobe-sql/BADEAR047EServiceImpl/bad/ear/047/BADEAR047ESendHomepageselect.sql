SELECT TASK_SECD /* 업무구분코드 */,
    TASK_SQNO /* 업무순번 */,
    REG_YMD /* 등록일자 */,
    REG_HMS /* 등록시분초 */,
    ETC_LBSVC_MNG_DEPT_CD /* 기타용역관리부서코드 */,
    F_DPT_INFO(ETC_LBSVC_MNG_DEPT_CD, 1) AS PRCS_DEPT_NM /* 처리부서명 */,
    ADT_MTTR_NM /* 감사사항명 */,
    ADT_MTTR_CN /* 감사사항내용 */,
    #N/A /* 문서아이디1 */,
    #N/A /* 문서아이디2 */,
    RLS_YN /* 공개여부 */,
    ADT_SQNO /* 감사순번 */,
    (SELECT AS USR_DFN_TXT_1
FROM TBDCMACM013C WHERE CMM_CD_DVSN_CD = '1000660' AND CD = ADT_FLD_SECD) AS AUD_SPH_HIRK_CD , ADT_FLD_SECD , REL_INST_CD ,#CON_HNDL_CD# AS LINK_STTS_SECD FROM TB_BADEAR028M WHERE TASK_SECD = #TASK_SECD# AND TASK_SQNO = #TASK_SQNO#