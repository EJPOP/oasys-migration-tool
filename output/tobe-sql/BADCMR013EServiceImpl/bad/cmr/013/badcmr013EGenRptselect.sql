SELECT INFO_YR /* 정보년도 */,
    RPRT_SNO,
    CHR_DPT_CD,
    PLCY_RPRT_KND_CD,
    RSCH_ASMT_TTL /* 연구과제제목 */,
    ORGTXT_CN /* 원문내용 */,
    CVLCPT_DOC_ID /* 민원문서아이디 */,
    ADT_PLAN_DOC_TPCD /* 감사계획문서유형코드 */,
    SAV_DH,
    PLCY_MDF_DH,
    RGTR_ID /* 등록자아이디 */,
    F_USR_INFO(RGTR_ID,'2') AS RGTR_NM /* 등록자명 */
FROM TBBADCMR009M WHERE 1=1 AND INFO_YR = #INFO_YR# AND PLCY_RPRT_KND_CD =#strRprtKnd#