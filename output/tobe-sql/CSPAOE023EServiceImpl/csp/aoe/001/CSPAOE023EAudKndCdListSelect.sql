SELECT CD /* 코드 */,
    CD_NM /* 코드명 */
FROM TBDCMACM013C WHERE 1=1 AND CD <> '000000000' AND USE_YN = 'Y' AND CMM_CD_DVSN_CD ='1000270' /*감사종류코드*/ AND CD NOT IN '10000' /*감사종류: 공통종류코드 제외*/