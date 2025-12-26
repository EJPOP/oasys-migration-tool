SELECT PRV_NM ||' '|| CITY_NM ||' '|| ROAD_NM ||' '||BULD_NO_MAIN_NO AS PRV_NM,
    PRV_NM ||' '|| CITY_NM || NVL2(TWN_NM,' ' || TWN_NM, '')||' '||NVL2(RI_NM,'' || RI_NM, '') ||LD_NM ||' '||ALN_MAIN_NO AS OTSD_EXPRT_ADDR /* 외부전문가주소 */,
    CRU_ZIP /* 부패우편번호 */,
    SUBSTR(CRU_ZIP,1,3) AS SUBSTR,
    SUBSTR(CRU_ZIP,4,3) AS SUBSTR
FROM TBDCMACM020L WHERE 1=1 AND (LD_NM LIKE #strName#||'%' OR TWN_NM LIKE #strName#||'%' OR RI_NM LIKE #strName#||'%' OR ROAD_NM LIKE #strName#||'%' OR CITY_BULD_NM LIKE '%'||#strName#||'%' )