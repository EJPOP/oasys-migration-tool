SELECT CVLCPT_ADT_YR /* 민원감사년도 */,
    ADT_KDCD /* 감사종류코드 */,
    CVLCPT_ADT_YR||'-'||TSK_NO AS CVLCPT_ADT_YR /* 민원감사년도 */,
    TSK_NO /* 업무번호 */,
    ADT_MTTR_NM /* 감사사항명 */,
    HNDL_DIV_DPT_CD /* 처리과 부서코드 */,
    MNDY_CNT /* 연인원 */
FROM TBCSPAOE021M WHERE 1=1 AND CVLCPT_ADT_YR = #CVLCPT_ADT_YR# AND AUD_ACTV_CON_YN IS NULL AND LENGTH(TSK_NO) > 3 AND HNDL_DIV_DPT_CD = #strUsrDptCd# ADT_MTTR_NM LIKE '%'||#strAudSbjNm#||'%' TSK_NO = #strTskNo# ORDER BY CVLCPT_ADT_YR, TSK_NO