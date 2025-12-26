SELECT INFO_YR /* 정보년도 */,
    INST_CD /* 기관코드 */,
    F_ORG_INFO(INST_CD,'1') AS PROF_RBPRSN_INST_NM /* 증명책임자기관명 */,
    TASK_SNO,
    INST_CD /* 기관코드 */,
    F_ORG_INFO(INST_CD,'5') AS OTSD_EXPRT_ADDR /* 외부전문가주소 */
FROM TBBADCMR017L WHERE 1=1 AND INFO_YR = #INFO_YR# AND TASK_SNO =#strTaskSno#