select D1.PIC_NM /* 담당자명 */,
    D1.CHR_DPT_CD,
    D1.TKCG_DEPT_NM /* 담당부서명 */,
    D1.PIC_ID /* 담당자아이디 */
from tbcspfpl002m d1, tbdcmacm001m D2 where 1=1 and D1.PIC_NM = D2.USER_NM and D1.PIC_ID = USER_ID and D1.TKCG_DEPT_NM = F_DPT_INFO (D2.SRECS_DEPT_CD,'1') D1.QSTNAI_YR = #strQstnaiYr# D1.QSTNAI_NO = #strQstnaiNo# D1.DTIL_ANS_NO = #strDtilAnsNo# D1.DTIL_DVSN_NO = #strDtilDvsnNo# ORDER BY D1.PIC_NM