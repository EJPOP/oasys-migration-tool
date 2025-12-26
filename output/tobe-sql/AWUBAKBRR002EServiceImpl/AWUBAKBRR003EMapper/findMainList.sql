/* AWU.BAK.BRR.AWUBAKBRR003EMAPPER.findMainList */ SELECT FRS_MDM_SQNO /* 포렌식매체순번 */,
    F_AWU_CODE_NM('10000006', SRECS_DCSN_SECD ) || '-' || SRECS_DCSN_YR || '-' || SRECS_DCSN_NO AS F_AWU_CODE_NM,
    SRECS_DCSN_YR /* 심사재심의결정년도 */,
    SRECS_DCSN_NO /* 심사재심의결정번호 */,
    SRECS_DCSN_SECD /* 심사재심의결정구분코드 */,
    F_AWU_CODE_NM('10000006', SRECS_DCSN_SECD ) AS F_AWU_CODE_NM,
    CLM_SECD /* 청구구분코드 */,
    F_AWU_CODE_NM('10000007', CLM_SECD ) AS F_AWU_CODE_NM,
    TO_CHAR(TO_DATE(SRECS_DCSN_YMD, 'YYYYMMDD'), 'YYYY-MM-DD') AS SRECS_DCSN_YMD /* 심사재심의결정일자 */,
    RSCH_ASMT_TTL /* 연구과제제목 */,
    SRECS_ISSUE_CN /* 심사재심의이슈내용 */,
    CVLCPT_DOC_ID /* 민원문서아이디 */,
    COUNT(1) OVER () AS 1
FROM TBAWUBAKBRR002M T1 WHERE 1=1 AND T1.SRECS_DCSN_SECD NOT IN ('1','4') AND T1.SRECS_DCSN_YR = #{srchDcsYr} AND T1.DCS_NO = #{srchDcsNo} AND SRECS_DCSN_SECD = #{srchDcsDvsnCd} AND REQ_DVSN_CD = #{srchReqDvsnCd} AND ( RSCH_ASMT_TTL LIKE '%'||#{srchTitOrIsuTxt}||'%' OR ISU_TXT LIKE '%'||#{srchTitOrIsuTxt}||'%' ) ORDER BY T1.FRS_MDM_SQNO DESC