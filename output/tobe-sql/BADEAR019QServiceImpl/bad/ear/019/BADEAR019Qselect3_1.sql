/*쿼리명 : SQL_Sub05*/ select BB.NM /* 명칭 */,
    NVL(TOT_BG_1,0) AS TOT_BG_1 /* 합계 기관건수 */,
    NVL(TOT_BG_2,0) AS TOT_BG_2 /* 합계 기관수 */,
    NVL(TOT_BP_1,0) AS TOT_BP_1 /* 합계 인원건수 */,
    NVL(TOT_BP_2,0) AS TOT_BP_2 /* 합계 인원수 */,
    NVL(AG_1,0) AS AG_1 /* 국가기관 기관건수 */,
    NVL(AG_2,0) AS AG_2 /* 국가기관 기관수 */,
    NVL(AP_1,0) AS AP_1 /* 국가기관 인원건수 */,
    NVL(AP_2,0) AS AP_2 /* 국가기관 인원수 */,
    NVL(BG_1,0) AS BG_1 /* 자치단체 기관건수 */,
    NVL(BG_2,0) AS BG_2 /* 자치단체 기관수 */,
    NVL(BP_1,0) AS BP_1 /* 자치단체 인원건수 */,
    NVL(BP_2,0) AS BP_2 /* 자치단체 인원수 */,
    NVL(CG_1,0) AS CG_1 /* 투자기관 기관건수 */,
    NVL(CG_2,0) AS CG_2 /* 투자기관 기관수 */,
    NVL(CP_1,0) AS CP_1 /* 투자기관 인원건수 */,
    NVL(CP_2,0) AS CP_2 /* 투자기관 인원수 */,
    NVL(DG_1,0) AS DG_1 /* 기타단체 기관건수 */,
    NVL(DG_2,0) AS DG_2 /* 기타단체 기관수 */,
    NVL(DP_1,0) AS DP_1 /* 기타단체 인원건수 */,
    NVL(DP_2,0) AS DP_2 /* 기타단체 인원수 */
from (select MASTER.DSPS_RQT_KDCD /* 처분요구종류코드 */,
    sum(case when MASTER.INST_SECD >= '10' then (case when DETAIL.NOP_CONT > 0 then 0 else 1 end) else 0 end) AS INST_SECD /* 기관구분코드 */,
    sum(case when MASTER.INST_SECD >= '10' then (case when DETAIL.NOP_CONT > 0 then 0 when DETAIL.NOP_CONT = '10' then (case when DETAIL.NOP_CONT > 0 then 1 else 0 end) else 0 end) as TOT_BP_1 ,sum(case when MASTER.INST_SECD >= '10' then (case when DETAIL.NOP_CONT > 0 then DETAIL.NOP_CONT else 0 end) else 0 end) as TOT_BP_2 ,sum(case when MASTER.INST_SECD = '10' then (case when DETAIL.NOP_CONT > 0 then 0 else 1 end) else 0 end) as AG_1 ,sum(case when MASTER.INST_SECD = '10' then (case when DETAIL.NOP_CONT > 0 then 0 when DETAIL.NOP_CONT 0 then 1 else 0 end) else 0 end) as AP_1 ,sum(case when MASTER.INST_SECD = '10' then (case when DETAIL.NOP_CONT > 0 then DETAIL.NOP_CONT else 0 end) else 0 end) as AP_2 ,sum(case when MASTER.INST_SECD in ('21', '22') then (case when DETAIL.NOP_CONT > 0 then 0 else 1 end) else 0 end) as BG_1 ,sum(case when MASTER.INST_SECD in ('21', '22') then (case when DETAIL.NOP_CONT > 0 then 0 when DETAIL.NOP_CONT 0 then 1 else 0 end) else 0 end) as BP_1 ,sum(case when MASTER.INST_SECD in ('21', '22') then (case when DETAIL.NOP_CONT > 0 then DETAIL.NOP_CONT else 0 end) else 0 end) as BP_2 ,sum(case when MASTER.INST_SECD = '30' then (case when DETAIL.NOP_CONT > 0 then 0 else 1 end) else 0 end) as CG_1 ,sum(case when MASTER.INST_SECD = '30' then (case when DETAIL.NOP_CONT > 0 then 0 when DETAIL.NOP_CONT 0 then 1 else 0 end) else 0 end) as CP_1 ,sum(case when MASTER.INST_SECD = '30' then (case when DETAIL.NOP_CONT > 0 then DETAIL.NOP_CONT else 0 end) else 0 end) as CP_2 ,sum(case when MASTER.INST_SECD not in ('10','21','22','30') then (case when DETAIL.NOP_CONT > 0 then 0 else 1 end) else 0 end) as DG_1 ,sum(case when MASTER.INST_SECD not in ('10','21','22','30') then (case when DETAIL.NOP_CONT > 0 then 0 when DETAIL.NOP_CONT 0 then 1 else 0 end) else 0 end) as DP_1 ,sum(case when MASTER.INST_SECD not in ('10','21','22','30') then (case when DETAIL.NOP_CONT > 0 then DETAIL.NOP_CONT else 0 end) else 0 end) AS INST_SECD /* 기관구분코드 */
from TBBADEAR002D detail ,TBBADEAR001M master where 1=1 and MASTER.CVLCPT_ADT_YR = DETAIL.CVLCPT_ADT_YR and MASTER.ADT_NO = DETAIL.ADT_NO and MASTER.DSPS_RQT_SQNO = DETAIL.DSPS_RQT_SQNO and MASTER.DSPS_RQT_KDCD in ('810','820','830','840','850') /*모범목록코드(고정)*/ and DETAIL.ENFC_YMD between #strFromDt# and #strToDt# /*화면의 기간(인자값)*/ and DETAIL.CFMTN_STTS_SECD != '0' group by MASTER.DSPS_RQT_KDCD ) AA, (select '810' AS CD,
    '예산절감' AS NM
FROM DUAL union all select '820' AS CD,
    '업무개선' AS NM
FROM DUAL union all select '830' AS CD,
    '성실근무' AS NM
FROM DUAL union all select '840' AS CD,
    '민원해소' AS NM
FROM DUAL union all select '850' AS CD,
    '모범(기타)' AS NM
FROM DUAL ) BB where AA.DSPS_RQT_KDCD(+) = BB.CD ORDER BY BB.CD