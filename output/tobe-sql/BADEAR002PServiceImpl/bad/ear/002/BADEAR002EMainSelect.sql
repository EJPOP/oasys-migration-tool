select D1.INDV_INFO_PRCS_SYM_NO /* 개인정보처리기호번호 */,
    D1.INDV_INFO_PRCS_SYM_CN /* 개인정보처리기호내용 */,
    D1.REG_YMD /* 등록일자 */,
    (select COUNT(INDV_INFO_PRCS_SYM_CN) AS INDV_INFO_PRCS_SYM_CN /* 개인정보처리기호내용 */
from TB_BADEAR010L) AS CNT from TB_BADEAR010L D1 where 1=1