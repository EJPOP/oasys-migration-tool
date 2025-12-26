select M.INST_SECD AS INST_SECD /* 기관구분코드 */,
    case when SUBSTR(M.INST_SECD, 1, 1) = '1' then '국가기관' when SUBSTR(M.INST_SECD, 1, 1) = '2' then '자치단체' when SUBSTR(M.INST_SECD, 1, 1) = '3' then '공공기관' else '기타단체' end AS SUBSTR,
    M.C1+M.C3+M.C5+M.C7 AS C1,
    M.C2+M.C6 AS C2,
    M.C1,
    M.C2,
    M.C3,
    M.C4,
    M.C5,
    M.C6,
    M.C7
from (select MASTER.INST_SECD AS INST_SECD /* 기관구분코드 */,
    sum(case when MASTER.DSPS_RQT_KDCD = '110' then 1 else 0 end ) AS DSPS_RQT_KDCD /* 처분요구종류코드 */,
    sum(case when MASTER.DSPS_RQT_KDCD = '110' then NVL(DETAIL.PNRT_PRSRV_AMT,0) else 0 end ) AS DSPS_RQT_KDCD /* 처분요구종류코드 */,
    sum(case when MASTER.DSPS_RQT_KDCD = '210' then 1 else 0 end ) AS DSPS_RQT_KDCD /* 처분요구종류코드 */,
    sum(case when MASTER.DSPS_RQT_KDCD = '210' then NVL(DETAIL.NOP_CONT,0) else 0 end ) AS DSPS_RQT_KDCD /* 처분요구종류코드 */,
    sum(case when SUBSTR(MASTER.DSPS_RQT_KDCD,1,1) = '3' then 1 else 0 end ) AS SUBSTR,
    sum(case when SUBSTR(MASTER.DSPS_RQT_KDCD,1,1) = '3' then NVL(DETAIL.PNRT_PRSRV_AMT,0) else 0 end ) AS SUBSTR,
    sum(case when SUBSTR(MASTER.DSPS_RQT_KDCD,1,1) = '5' then 1 else 0 end ) AS SUBSTR
from TBBADEAR001M master ,TBBADEAR002D detail ,(select CVLCPT_ADT_YR /* 민원감사년도 */,
    ADT_NO /* 감사번호 */,
    DSPS_RQT_SQNO /* 처분요구순번 */,
    COUNT(*) AS CNT
from TBBADFRE009M where 1=1 and IMPL_URG_YMD between #strFromDt# and #strToDt# /*화면의 인자값 기간*/ group by CVLCPT_ADT_YR ,ADT_NO ,DSPS_RQT_SQNO ) prss where 1=1 and MASTER.CVLCPT_ADT_YR = DETAIL.CVLCPT_ADT_YR and MASTER.ADT_NO = DETAIL.ADT_NO and MASTER.DSPS_RQT_SQNO = DETAIL.DSPS_RQT_SQNO and MASTER.CVLCPT_ADT_YR = PRSS.CVLCPT_ADT_YR and MASTER.ADT_NO = PRSS.ADT_NO and MASTER.DSPS_RQT_SQNO = PRSS.DSPS_RQT_SQNO and DETAIL.ENFC_YMD between #strFromDt# and #strToDt# /*화면의 인자값 기간*/ and DETAIL.CFMTN_STTS_SECD !='2' and prss.CNT >= #strPressCnt# /*화면의 인자값 독촉회수 1이상이면 :1, 독촉회수 2이상이면:2*/ group by MASTER.INST_SECD ) M order by M.INST_SECD