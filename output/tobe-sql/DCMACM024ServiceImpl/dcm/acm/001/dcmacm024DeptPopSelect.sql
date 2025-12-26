SELECT SRECS_DEPT_CD /* 심사재심의부서코드 */,
    CNSTN_MBCMT_DEPT_NM /* 자문위원부서명 */,
    CRU_TELNO /* 부패전화번호 */,
    USE_YN /* 사용여부 */,
    ABR_DPT_NM_1,
    ABR_DPT_NM_2,
    FRML_DPT_NM,
    STND_DPT_CD,
    CNSTN_MBCMT_FAX_NO /* 자문위원팩스번호 */,
    OW_DVDT_TXT,
    DEPT_TPCD /* 부서유형코드 */,
    IMD_CD,
    DPT_SNO,
    CRU_ZIP /* 부패우편번호 */,
    OTSD_EXPRT_ADDR /* 외부전문가주소 */,
    WEB_SITE_ADDR /* , SAL_DPT_CD 2016.06.30 /*, SAL_DPT_NM 2016.06.30 AS WEB_SITE_ADDR /* 웹사이트주소 */,
    HIRK_DPT_CD
FROM TBDCMACM002M WHERE 1=1 AND SRECS_DEPT_CD IN ( SELECT SRECS_DEPT_CD /* 심사재심의부서코드 */
FROM TABLE(F_AUTH_DEPT(#strUsrCnb#, #strUsrDptAuth#, 'Y'))) SRECS_DEPT_CD LIKE '%'||#strDptCd# ||'%' (CNSTN_MBCMT_DEPT_NM LIKE '%'||#strDptNm# ||'%' OR ABR_DPT_NM_1 LIKE '%'||#strDptNm# ||'%' OR ABR_DPT_NM_2 LIKE '%'||#strDptNm# ||'%' OR SRECS_DEPT_CD LIKE '%'||#strDptNm#||'%' OR FRML_DPT_NM LIKE '%'||#strDptNm#||'%') USE_YN LIKE '%'||#strUseYn# ||'%' ORDER BY DPT_SNO, SRECS_DEPT_CD