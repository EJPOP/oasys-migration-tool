/*쿼리명 : SQL_1*/ select ECD.CD_NM,
    /*구분*/ (NVL(aft.MAN_DAY, 0 ) - NVL(before.MAN_DAY, 0)) AS MAN_DAY,
    /*연인원*/ (NVL(AFT.LEAD_CST, 0) - NVL(BEFORE.LEAD_CST, 0)) AS LEAD_CST /* 지휘비용 */,
    /*지휘비*/ (NVL(AFT.SPM_CST, 0) - NVL(BEFORE.SPM_CST, 0)) AS SPM_CST /* 보충비용 */,
    /*보충경비*/ (NVL(aft.SUM_ALL, 0) - NVL(before.SUM_ALL, 0)) AS SUM_ALL /* 계 */
from TBDCMACM013C ecd, (select RGN_SECD /* 지역구분코드 */,
    sum(BZTRP_DCN) AS BZTRP_DCN /* 출장일수 */,
    sum(LEAD_CST) AS LEAD_CST /* 지휘비용 */,
    sum(SPM_CST) AS SPM_CST /* 보충비용 */,
    sum(LEAD_CST) + sum(SPM_CST) AS LEAD_CST /* 지휘비용 */
from TBBADDED011L where BZTRP_YR = #BZTRP_YR# and BZTRP_SQNO =#BZTRP_SQNO# and CAL_YN = 'N' group by RGN_SECD) before, (select RGN_SECD /* 지역구분코드 */,
    sum(BZTRP_DCN) AS BZTRP_DCN /* 출장일수 */,
    sum(LEAD_CST) AS LEAD_CST /* 지휘비용 */,
    sum(SPM_CST) AS SPM_CST /* 보충비용 */,
    sum(LEAD_CST) + sum(SPM_CST) AS LEAD_CST /* 지휘비용 */
from TBBADDED011L where BZTRP_YR = #BZTRP_YR# and BZTRP_SQNO = #BZTRP_SQNO# and CAL_YN = 'Y' group by RGN_SECD) aft where ECD.CMM_CD_DVSN_CD='1000023' and ECD.CD in ('1', '2', '3') and BEFORE.RGN_SECD(+) = ECD.CD and AFT.RGN_SECD(+) = ECD.CD ORDER BY 1