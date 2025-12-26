/*쿼리명 : SQL_2*/ select aggr.*
from (SELECT TER.BZTRP_YR /* 출장년도 */,
    TER.BZTRP_SQNO /* 출장순번 */,
    TER.BZTRVR_ENO /* 출장자직원전산번호 */,
    /*전산번호*/ TER.BZTRVR_NM AS BZTRVR_NM /* 출장자명 */,
    /*성명 */ F_CODE_NM('1000173',TER.BZTRVR_JBGD_SECD) AS F_CODE_NM,
    /* 직급명*/ F_DPT_INFO(BL.OGDP_DEPT_CD,'4') AS CNSTN_MBCMT_DEPT_NM /* 자문위원부서명 */,
    /*소속 */ TER.EMP_SECD AS EMP_SECD /* 직원구분코드 */,
    TER.DPTRE_RGN_SECD /* 출발지역구분코드 */,
    te.IN_EXP,
    /*여비*/ TE.BZTRP_DCN AS BZTRP_DCN /* 출장일수 */,
    /*일수*/ NVL(TFD.RL_FARE_AMT, 0) + NVL(TE.CPRG_FARE_CST,0) AS RL_FARE_AMT /* 철도운임금액 */,
    /*철도임+수도권운임비*/ NVL(TFD.RL_FARE_AMT, 0) + + NVL(TE.CPRG_FARE_CST,0) + te.IN_EXP AS RL_FARE_AMT /* 철도운임금액 */,
    /*지급액*/ TE.BZTRP_BGNG_YMD AS BZTRP_BGNG_YMD /* 출장시작일자 */,
    /*출장기간FROM*/ TE.BZTRP_END_YMD AS BZTRP_END_YMD /* 출장종료일자 */,
    /*출장기간TO*/ '' AS RECIPIENT /* 영수인 */
FROM TBBADDED010L ter, TBBADDED002M situ, (select BZTRP_YR /* 출장년도 */,
    BZTRP_SQNO /* 출장순번 */,
    BZTRVR_ENO /* 출장자직원전산번호 */,
    sum( case when TE0.CAL_YN='Y' then TE0.BZTRP_TRVEXP_FDEXP + TE0.BTEXP_CRTR_AMT + TE0.LDG_CST else 0 end) - sum( case when TE0.CAL_YN='N' then TE0.BZTRP_TRVEXP_FDEXP + TE0.BTEXP_CRTR_AMT + TE0.LDG_CST else 0 end) AS CAL_YN /* 계산여부 */,
    /* 수도권운임 추가 - 2015.05.06 yyh*/ SUM( CASE WHEN TE0.CAL_YN='Y' THEN NVL(TE0.CPRG_FARE_CST,0) ELSE 0 END) - SUM( CASE WHEN TE0.CAL_YN='N' THEN NVL(TE0.CPRG_FARE_CST,0) ELSE 0 END) AS CPRG_FARE_CST /* 수도권운임비용 */,
    sum( case when TE0.CAL_YN='Y' then TE0.BZTRP_DCN else 0 end) - sum( case when TE0.CAL_YN='N' then TE0.BZTRP_DCN else 0 end) AS BZTRP_DCN /* 출장일수 */,
    min(BZTRP_BGNG_YMD) AS BZTRP_BGNG_YMD /* 출장시작일자 */,
    max(BZTRP_END_YMD) AS BZTRP_END_YMD /* 출장종료일자 */
from TBBADDED011L te0 where BZTRP_YR = #BZTRP_YR# and BZTRP_SQNO = #BZTRP_SQNO# group by BZTRP_YR, BZTRP_SQNO, BZTRVR_ENO) te, (select BZTRP_YR /* 출장년도 */,
    BZTRP_SQNO /* 출장순번 */,
    BZTRVR_ENO /* 출장자직원전산번호 */,
    sum( case when CAL_YN='Y' then RL_FARE_AMT else 0 end) - sum( case when CAL_YN='N' then RL_FARE_AMT else 0 end) AS RL_FARE_AMT /* 철도운임금액 */
from TBBADDED017L where BZTRP_YR =#BZTRP_YR# and BZTRP_SQNO = #BZTRP_SQNO# group by BZTRP_YR, BZTRP_SQNO, BZTRVR_ENO ) tfd, (select BZTRP_YR /* 출장년도 */,
    BZTRP_SQNO /* 출장순번 */,
    BZTRVR_ENO /* 출장자직원전산번호 */,
    MAX(OGDP_DEPT_CD) AS OGDP_DEPT_CD /* 소속부서코드 */
from TBBADDED005L where BZTRP_YR = #BZTRP_YR# and BZTRP_SQNO = #BZTRP_SQNO# group by BZTRP_YR, BZTRP_SQNO, BZTRVR_ENO) DBO_BLL_BLNC WHERE TER.BZTRP_YR = #BZTRP_YR# and TER.BZTRP_SQNO = #BZTRP_SQNO# and TE.BZTRP_YR = TER.BZTRP_YR and TE.BZTRP_SQNO = TER.BZTRP_SQNO and TE.BZTRVR_ENO = TER.BZTRVR_ENO and TFD.BZTRP_YR(+) = TER.BZTRP_YR and TFD.BZTRP_SQNO(+) = TER.BZTRP_SQNO and TFD.BZTRVR_ENO(+) = TER.BZTRVR_ENO and SITU.BZTRP_YR = TER.BZTRP_YR and SITU.BZTRP_SQNO = TER.BZTRP_SQNO and BL.BZTRP_YR(+) = TER.BZTRP_YR and BL.BZTRP_SQNO(+) = TER.BZTRP_SQNO and BL.BZTRVR_ENO(+) = TER.BZTRVR_ENO ) aggr where aggr.SUM_ALL !=0