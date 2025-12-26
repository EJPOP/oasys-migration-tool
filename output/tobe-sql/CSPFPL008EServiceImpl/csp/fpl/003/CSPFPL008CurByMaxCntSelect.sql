SELECT ( select COUNT(*)
from tbcspfpl002m where qstnai_yr = #strQstnaiYr# and qstnai_no = #strQstnaiNo# and NTAS_RQ_MTRL_HNDL_STA_CD = '50') || '/' || ( SELECT COUNT(*)
FROM TBCSPFPL002M where qstnai_yr = #strQstnaiYr# and qstnai_no = #strQstnaiNo#) AS BB FROM DUAL;