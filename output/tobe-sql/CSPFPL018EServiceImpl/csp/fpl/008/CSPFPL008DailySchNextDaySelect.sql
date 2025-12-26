SELECT min(WRT_YMD) AS WRT_YMD /* 작성일자 */,
    case to_char(to_date(min(WRT_YMD), 'YYYYMMDD'), 'd') when 1 then '일' when 2 then '월' when 3 then '화' when 4 then '수' when 5 then '목' when 6 then '금' when 7 then '토' end AS TO_CHAR
from tbcspfpl005m where 1=1 and WRT_YMD > #strDate# AND INCD_DVSN_CD = 'A1'