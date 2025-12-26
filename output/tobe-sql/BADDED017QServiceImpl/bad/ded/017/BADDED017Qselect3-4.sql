SELECT BZTRP_YR /* 출장년도 */,
    BZTRP_SQNO /* 출장순번 */,
    BZTRVR_ENO /* 출장자직원전산번호 */,
    F_CODE_NM('1000001',TE.RGN_CD) AS F_CODE_NM,
    /*기존지역*/ BZTRP_BGNG_YMD AS BZTRP_BGNG_YMD /* 출장시작일자 */,
    /*시작일*/ BZTRP_END_YMD AS BZTRP_END_YMD /* 출장종료일자 */,
    /*종료일*/ BZTRP_DCN AS BZTRP_DCN /* 출장일수 */,
    /*일수*/ LDG_DCN AS LDG_DCN /* 숙박일수 */
FROM TB_BADDED011L TE WHERE TE.BZTRP_YR = #BZTRP_YR# AND TE.BZTRP_SQNO = #BZTRP_SQNO# AND TE.CAL_YN = 'Y' AND TE.BZTRVR_ENO = #BZTRVR_ENO#