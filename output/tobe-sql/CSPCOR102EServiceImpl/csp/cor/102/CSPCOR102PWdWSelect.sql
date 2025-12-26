SELECT T1.CVLCPT_SECD /* 민원구분코드 */,
    T1.CVLCPT_RCPT_YR /* 민원접수년도 */,
    T1.CVLCPT_RCPT_NO /* 민원접수번호 */,
    T1.CRU_DISCON_RSN_CN /* 부패취하사유내용 */,
    T1.CRU_PRCS_STTS_SECD /* 부패처리상태구분코드 */,
    T1.CRU_TELNO /* 부패전화번호 */,
    T1.CRU_ACTL_RCPT_NO /* 부패실제접수번호 */,
    T1.CRU_HMPG_APLY_NO /* 부패누리집신청번호 */
FROM TB_CSPCOR101M T1 WHERE T1.CVLCPT_SECD = #CVLCPT_SECD# AND T1.CVLCPT_RCPT_YR = #CVLCPT_RCPT_YR# AND T1.CVLCPT_RCPT_NO = #CVLCPT_RCPT_NO#