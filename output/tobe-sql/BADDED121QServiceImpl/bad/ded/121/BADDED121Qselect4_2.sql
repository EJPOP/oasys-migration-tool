/*쿼리명 : SQL_2*/ select aggr.*
from (SELECT TER.BZTRP_YR /* 출장년도 */,
    TER.BZTRP_SQNO /* 출장순번 */,
    TER.BZTRVR_ENO /* 출장자직원전산번호 */,
    /*전산번호*/ TER.BZTRVR_NM AS BZTRVR_NM /* 출장자명 */,
    /*성명*/ TER.BZTRVR_JBGD_SECD AS BZTRVR_JBGD_SECD /* 출장자직급구분코드 */,
    F_CODE_NM('1000173',TER.BZTRVR_JBGD_SECD) AS F_CODE_NM,
    /* 직급*/ F_DPT_INFO(BL.OGDP_DEPT_CD,'4') AS CNSTN_MBCMT_DEPT_NM /* 자문위원부서명 */,
    /*소속 */ TER.EMP_SECD AS EMP_SECD /* 직원구분코드 */,
    TER.DPTRE_RGN_SECD /* 출발지역구분코드 */,
    TE.SPM_CST /* 보충비용 */,
    /*보충경비*/ TE.BZTRP_DCN AS BZTRP_DCN /* 출장일수 */,
    /*출장일수*/ TE.BZTRP_BGNG_YMD AS BZTRP_BGNG_YMD /* 출장시작일자 */,
    /*출장시작일*/ TE.BZTRP_END_YMD AS BZTRP_END_YMD /* 출장종료일자 */,
    /*출장종료일*/ '' AS RECIPIENT,
    /*영수인*/ ( case when SITU.BZTRP_TL_ENO = TER.BZTRVR_ENO then TER2.LEAD_CST else 0 end) AS LEAD_CST /* 지휘비용 */,
    /*지휘비*/ TE.SPM_CST + ( case when SITU.BZTRP_TL_ENO = TER.BZTRVR_ENO then TER2.LEAD_CST else 0 end) AS SPM_CST /* 보충비용 */
FROM TBBADDED010L ter, TBBADDED002M situ, (select BZTRP_YR /* 출장년도 */,
    BZTRP_SQNO /* 출장순번 */,
    BZTRVR_ENO /* 출장자직원전산번호 */,
    sum( case when TE0.CAL_YN='Y' then TE0.SPM_CST else 0 end) - sum( case when TE0.CAL_YN='N' then TE0.SPM_CST else 0 end) AS SPM_CST /* 보충비용 */,
    sum( case when TE0.CAL_YN='Y' then TE0.BZTRP_DCN else 0 end) - sum( case when TE0.CAL_YN='N' then TE0.BZTRP_DCN else 0 end) AS BZTRP_DCN /* 출장일수 */,
    min(BZTRP_BGNG_YMD) AS BZTRP_BGNG_YMD /* 출장시작일자 */,
    max(BZTRP_END_YMD) AS BZTRP_END_YMD /* 출장종료일자 */
from TBBADDED011L te0 where BZTRP_YR = #BZTRP_YR# and BZTRP_SQNO = #BZTRP_SQNO# group by BZTRP_YR, BZTRP_SQNO, BZTRVR_ENO) te, (select BZTRP_YR /* 출장년도 */,
    BZTRP_SQNO /* 출장순번 */,
    sum( case when CAL_YN='Y' then LEAD_CST else 0 end) - sum( case when CAL_YN='N' then LEAD_CST else 0 end) AS LEAD_CST /* 지휘비용 */
from TBBADDED011L where BZTRP_YR = #BZTRP_YR# and BZTRP_SQNO = #BZTRP_SQNO# group by BZTRP_YR, BZTRP_SQNO) ter2, (select BZTRP_YR /* 출장년도 */,
    BZTRP_SQNO /* 출장순번 */,
    BZTRVR_ENO /* 출장자직원전산번호 */,
    MAX(OGDP_DEPT_CD) AS OGDP_DEPT_CD /* 소속부서코드 */
from TBBADDED005L where BZTRP_YR = #BZTRP_YR# and BZTRP_SQNO = #BZTRP_SQNO# group by BZTRP_YR, BZTRP_SQNO, BZTRVR_ENO) DBO_BLL_BLNC WHERE TER.BZTRP_YR = #BZTRP_YR# and TER.BZTRP_SQNO =#BZTRP_SQNO# and TE.BZTRP_YR = TER.BZTRP_YR and TE.BZTRP_SQNO = TER.BZTRP_SQNO and TE.BZTRVR_ENO = TER.BZTRVR_ENO and TER2.BZTRP_YR = TER.BZTRP_YR and TER2.BZTRP_SQNO = TER.BZTRP_SQNO and SITU.BZTRP_YR = TER.BZTRP_YR and SITU.BZTRP_SQNO = TER.BZTRP_SQNO and BL.BZTRP_YR(+) = TER.BZTRP_YR and BL.BZTRP_SQNO(+) = TER.BZTRP_SQNO and BL.BZTRVR_ENO(+) = TER.BZTRVR_ENO ) aggr where aggr.SUM_ALL !=0