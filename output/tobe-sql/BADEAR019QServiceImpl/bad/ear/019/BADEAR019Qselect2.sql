/*쿼리명 : SQL_Main04*/ select CASE WHEN INST_SECD ='10' THEN '국가기관' WHEN INST_SECD ='21' THEN '자치단체' WHEN INST_SECD ='30' THEN '공공기관' WHEN INST_SECD ='40' THEN '기타단체' END AS INST_SECD /* 기관구분코드 */,
    NVL(AA.TOT,0) AS TOT,
    NVL(AA.PA,0) AS PA,
    NVL(AA.GANG,0) AS GANG,
    NVL(AA.HAE,0) AS HAE,
    NVL(AA.JUNG,0) AS JUNG,
    NVL(AA.BU,0) AS BU
from (select case when MASTER.INST_SECD in ('21','22') then '21' when MASTER.INST_SECD in ('10','30') then MASTER.INST_SECD else '40' end AS INST_SECD /* 기관구분코드 */,
    sum(case when RELPSN.ACTN_RQT_KDCD = '001' then 1 else 0 end) + sum(case when RELPSN.ACTN_RQT_KDCD = '002' then 1 else 0 end) + sum(case when RELPSN.ACTN_RQT_KDCD = '003' then 1 else 0 end) + sum(case when RELPSN.ACTN_RQT_KDCD = '004' then 1 else 0 end) + sum(case when RELPSN.ACTN_RQT_KDCD = '013' then 1 else 0 end) AS ACTN_RQT_KDCD /* 조치요구종류코드 */,
    sum(case when RELPSN.ACTN_RQT_KDCD = '001' then 1 else 0 end) AS ACTN_RQT_KDCD /* 조치요구종류코드 */,
    sum(case when RELPSN.ACTN_RQT_KDCD = '013' then 1 else 0 end) AS ACTN_RQT_KDCD /* 조치요구종류코드 */,
    sum(case when RELPSN.ACTN_RQT_KDCD = '002' then 1 else 0 end) AS ACTN_RQT_KDCD /* 조치요구종류코드 */,
    sum(case when RELPSN.ACTN_RQT_KDCD = '003' then 1 else 0 end) AS ACTN_RQT_KDCD /* 조치요구종류코드 */,
    sum(case when RELPSN.ACTN_RQT_KDCD = '004' then 1 else 0 end) AS ACTN_RQT_KDCD /* 조치요구종류코드 */
from TBBADEAR001M master, TBBADEAR002D detail, TBBADEAR007M relpsn where MASTER.CVLCPT_ADT_YR = DETAIL.CVLCPT_ADT_YR and MASTER.ADT_NO = DETAIL.ADT_NO and MASTER.DSPS_RQT_SQNO = DETAIL.DSPS_RQT_SQNO and MASTER.CVLCPT_ADT_YR = RELPSN.CVLCPT_ADT_YR and MASTER.ADT_NO = RELPSN.ADT_NO and MASTER.DSPS_RQT_SQNO = RELPSN.DSPS_RQT_SQNO and MASTER.DSPS_RQT_KDCD = '210' and DETAIL.CFMTN_STTS_SECD != '0' and DETAIL.ENFC_YMD between #strFromDt# and #strToDt# group by case when MASTER.INST_SECD in ('21','22') then '21' when MASTER.INST_SECD in ('10','30') then MASTER.INST_SECD else '40' end ) AA ORDER BY INST_SECD