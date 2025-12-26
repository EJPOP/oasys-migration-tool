/*쿼리명 :SQL_3*/ select ECD.CD_NM,
    /*구분*/ (NVL(aft.MAN_DAY, 0 ) - NVL(before.MAN_DAY, 0)) AS MAN_DAY,
    /*연인원*/ (NVL(aft.IN_EXP, 0) - NVL(before.IN_EXP, 0)) AS IN_EXP,
    /*국내경비*/ (NVL(AFT.RL_FARE_AMT, 0) - NVL(BEFORE.RL_FARE_AMT, 0)) AS RL_FARE_AMT /* 철도운임금액 */,
    /*철도비*/ (NVL(aft.SUM_ALL, 0) - NVL(before.SUM_ALL, 0)) AS SUM_ALL /* 계 */
from TBDCMACM013C ecd, (select RGN_SECD /* 지역구분코드 */,
    sum(TE.BZTRP_DCN) AS BZTRP_DCN /* 출장일수 */,
    sum(TE.BZTRP_TRVEXP_FDEXP)+ sum(TE.BTEXP_CRTR_AMT) + sum(TE.LDG_CST) AS BZTRP_TRVEXP_FDEXP /* 출장여비식비 */,
    NVL(sum(TFD.RL_FARE_AMT), 0) + NVL(sum(TE.CPRG_FARE_CST),0) AS RL_FARE_AMT /* 철도운임금액 */,
    /*2015.05.11 - 수도권운임추가 yyh*/ sum(TE.BZTRP_TRVEXP_FDEXP)+ sum(TE.BTEXP_CRTR_AMT) + sum(TE.LDG_CST) +NVL(sum(TFD.RL_FARE_AMT), 0) + NVL(sum(TE.CPRG_FARE_CST),0) AS BZTRP_TRVEXP_FDEXP /* 출장여비식비 */
from TBBADDED011L te, (select BZTRP_YR /* 출장년도 */,
    BZTRP_SQNO /* 출장순번 */,
    BZTRVR_ENO /* 출장자직원전산번호 */,
    BTEXP_SQNO /* 출장비순번 */,
    CAL_YN /* 계산여부 */,
    sum(RL_FARE_AMT) AS RL_FARE_AMT /* 철도운임금액 */
from TBBADDED017L where BZTRP_YR = #BZTRP_YR# and BZTRP_SQNO = #BZTRP_SQNO# group by BZTRP_YR, BZTRP_SQNO, BZTRVR_ENO, BTEXP_SQNO, CAL_YN ) tfd where TE.BZTRP_YR =#BZTRP_YR# and TE.BZTRP_SQNO =#BZTRP_SQNO# and TE.CAL_YN = 'N' and TFD.BZTRP_YR(+) = TE.BZTRP_YR and TFD.BZTRP_SQNO(+) = TE.BZTRP_SQNO and TFD.BZTRVR_ENO(+) = TE.BZTRVR_ENO and TFD.BTEXP_SQNO(+) = TE.BTEXP_SQNO and TFD.CAL_YN(+) = TE.CAL_YN group by TE.RGN_SECD) before, (select RGN_SECD /* 지역구분코드 */,
    sum(TE.BZTRP_DCN) AS BZTRP_DCN /* 출장일수 */,
    sum(TE.BZTRP_TRVEXP_FDEXP)+ sum(TE.BTEXP_CRTR_AMT) + sum(TE.LDG_CST) AS BZTRP_TRVEXP_FDEXP /* 출장여비식비 */,
    NVL(sum(TFD.RL_FARE_AMT), 0) + NVL(sum(TE.CPRG_FARE_CST),0) AS RL_FARE_AMT /* 철도운임금액 */,
    /*2015.05.11 - 수도권운임추가 yyh*/ sum(TE.BZTRP_TRVEXP_FDEXP)+ sum(TE.BTEXP_CRTR_AMT) + sum(TE.LDG_CST) +NVL(sum(TFD.RL_FARE_AMT), 0) + NVL(sum(TE.CPRG_FARE_CST),0) AS BZTRP_TRVEXP_FDEXP /* 출장여비식비 */
from TBBADDED011L te, (select BZTRP_YR /* 출장년도 */,
    BZTRP_SQNO /* 출장순번 */,
    BZTRVR_ENO /* 출장자직원전산번호 */,
    BTEXP_SQNO /* 출장비순번 */,
    CAL_YN /* 계산여부 */,
    sum(RL_FARE_AMT) AS RL_FARE_AMT /* 철도운임금액 */
from TBBADDED017L where BZTRP_YR = #BZTRP_YR# and BZTRP_SQNO =#BZTRP_SQNO# group by BZTRP_YR, BZTRP_SQNO, BZTRVR_ENO, BTEXP_SQNO, CAL_YN ) tfd where TE.BZTRP_YR = #BZTRP_YR# and TE.BZTRP_SQNO = #BZTRP_SQNO# and TE.CAL_YN = 'Y' and TFD.BZTRP_YR(+) = TE.BZTRP_YR and TFD.BZTRP_SQNO(+) = TE.BZTRP_SQNO and TFD.BZTRVR_ENO(+) = TE.BZTRVR_ENO and TFD.BTEXP_SQNO(+) = TE.BTEXP_SQNO and TFD.CAL_YN(+) = TE.CAL_YN group by TE.RGN_SECD) aft where ECD.CMM_CD_DVSN_CD='1000023' and ECD.CD in ('1', '2', '3') and BEFORE.RGN_SECD(+) = ECD.CD and AFT.RGN_SECD(+) = ECD.CD ORDER BY 1