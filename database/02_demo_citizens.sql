-- Datos enteramente ficticios para una base de laboratorio.
-- Las cuentas y sus hashes se crean desde DemoData al activar el perfil lab.
BEGIN;
INSERT INTO citizens (document_id, first_name, last_name, birth_date, birth_place, issue_date, issue_place, rh, blood_group, height_cm, required) VALUES
('1000000011','Laura','Cárdenas','1993-02-14','Bogotá','2011-03-02','Bogotá','+','O',164,false),
('1000000012','Mateo','Salazar','1988-07-21','Medellín','2006-09-10','Medellín','+','A',176,true),
('1000000013','Valentina','Rojas','1999-11-05','Cali','2017-12-01','Cali','-','B',159,false),
('1000000014','Sebastián','Pineda','1985-04-17','Barranquilla','2003-05-22','Barranquilla','+','AB',181,true),
('1000000015','Camila','Torres','1996-09-03','Bucaramanga','2014-10-15','Bucaramanga','+','B',167,false),
('1000000016','Nicolás','Herrera','1991-12-29','Pereira','2010-01-13','Pereira','-','O',174,false),
('1000000017','Daniela','Acosta','2000-06-09','Manizales','2018-07-19','Manizales','+','A',162,true),
('1000000018','Samuel','Vargas','1987-03-26','Cartagena','2005-04-28','Cartagena','+','O',179,false),
('1000000019','Isabella','Mendoza','1995-08-12','Tunja','2013-09-04','Tunja','+','AB',165,false),
('1000000020','Andrés','Quintero','1990-01-30','Ibagué','2008-02-25','Ibagué','-','A',172,true)
ON CONFLICT (document_id) DO NOTHING;
COMMIT;
