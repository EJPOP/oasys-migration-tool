SELECT BZTRP_YR /* 출장년도 */,
    BZTRP_SQNO /* 출장순번 */,
    BZTRVR_ENO /* 출장자직원전산번호 */,
    BTEXP_SQNO /* 출장비순번 */,
    INST_CD /* 기관코드 */,
    RGN_CD /* 지역코드 */,
    RGN_SECD /* 지역구분코드 */,
    BZTRP_BGNG_YMD AS BZTRP_BGNG_YMD /* 출장시작일자 */,
    BZTRP_END_YMD AS BZTRP_END_YMD /* 출장종료일자 */
FROM TB_BADDED011L WHERE 1=1 AND CAL_YN = 'Y' AND BZTRP_YR = #BZTRP_YR# AND BZTRP_SQNO = #BZTRP_SQNO# AND BZTRVR_ENO = #BZTRVR_ENO# ORDER BY BZTRP_BGNG_YMD