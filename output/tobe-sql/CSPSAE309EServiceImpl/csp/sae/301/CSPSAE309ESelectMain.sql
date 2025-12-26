SELECT A.DMT_NO,
    A.DMT_DVSN_CD,
    A.RMN_NM,
    A.RCV_NOP_CNT,
    A.SEAT_NM,
    A.TKCG_DEPT_NM /* 담당부서명 */,
    A.PIC_NM /* 담당자명 */,
    A.USE_PPS_TXT,
    A.ESN,
    A.USE_YN /* 사용여부 */,
    A.RMRK_CN /* 비고내용 */
FROM TBCSPSAE115B A WHERE 1=1 AND A.DMT_DVSN_CD = #strDmtDvsn# AND A.USE_YN = #USE_YN# ORDER BY TO_NUMBER(A.RMN_NM)