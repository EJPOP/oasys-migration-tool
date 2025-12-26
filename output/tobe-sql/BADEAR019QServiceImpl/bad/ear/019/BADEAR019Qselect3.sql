/*쿼리명 : SQL_Main05*/ select sum(case when MASTER.INST_SECD >= '10' then 1 else 0 end) AS INST_SECD /* 기관구분코드 */,
    sum(case when MASTER.INST_SECD = '10' then 1 else 0 end) AS INST_SECD /* 기관구분코드 */,
    sum(case when MASTER.INST_SECD in ('21','22') then 1 else 0 end) AS INST_SECD /* 기관구분코드 */,
    sum(case when MASTER.INST_SECD = '30' then 1 else 0 end) AS INST_SECD /* 기관구분코드 */,
    sum(case when MASTER.INST_SECD not in ('10','21','22','30') then 1 else 0 end) AS INST_SECD /* 기관구분코드 */
from TB_BADEAR002D detail ,TB_BADEAR001M master where 1=1 and MASTER.ADT_YR = DETAIL.ADT_YR and MASTER.ADT_NO = DETAIL.ADT_NO and MASTER.DSPS_RQT_SQNO = DETAIL.DSPS_RQT_SQNO and MASTER.DSPS_RQT_KDCD in ('810','820','830','840','850') and DETAIL.CFMTN_STTS_SECD != '0' and DETAIL.ENFC_YMD between #strFromDt# and #strToDt# /*화면의 기간(인자값)*/