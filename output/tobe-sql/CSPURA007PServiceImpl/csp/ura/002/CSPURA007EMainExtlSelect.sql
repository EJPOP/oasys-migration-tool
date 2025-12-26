SELECT OTSD_EXPRT_NM /* 외부전문가명 */,
    CNSTN_MBCMT_DEPT_NM /* 자문위원부서명 */,
    JBPS_NM /* 직위명 */,
    CNSTN_MBCMT_MJR_NM /* 자문위원전공명 */
FROM TB_CSPURA003M WHERE OTSD_EXPRT_NM LIKE '%'||#NAM#||'%'