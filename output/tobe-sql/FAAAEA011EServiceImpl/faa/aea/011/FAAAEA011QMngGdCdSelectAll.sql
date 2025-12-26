SELECT A.INST_CD /* 기관코드 */,
    /*관리부서ID*/ A.MNG_GD_CD,
    /*관리부서ID*/ A.MNG_GD_NM /* 관리부서명 */
FROM TBFAAAEA048C A WHERE 1=1 AND A.INST_CD != A.MNG_GD_CD