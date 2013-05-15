
-- --------------------------------------------------
-- Entity Designer DDL Script for SQL Server 2005, 2008, and Azure
-- --------------------------------------------------
-- Date Created: 04/09/2012 19:28:06
-- Generated from EDMX file: C:\Users\Dave\Documents\Visual Studio 2010\Projects\OPKO_GPR_Database\OPKO_GPR_Database\DBModel.edmx
-- --------------------------------------------------

SET QUOTED_IDENTIFIER OFF;
GO
USE [Twodoku];
GO
IF SCHEMA_ID(N'dbo') IS NULL EXECUTE(N'CREATE SCHEMA [dbo]');
GO

-- --------------------------------------------------
-- Dropping existing FOREIGN KEY constraints
-- --------------------------------------------------


-- --------------------------------------------------
-- Dropping existing tables
-- --------------------------------------------------

IF OBJECT_ID(N'[dbo].[Games]', 'U') IS NOT NULL
    DROP TABLE [dbo].[Games];
GO


-- --------------------------------------------------
-- Creating all tables
-- --------------------------------------------------

-- Creating table 'Calibrations'
CREATE TABLE [dbo].[Games] (
    [GAMEID] int  NOT NULL,
    [PLAYER1] varchar(max)  NOT NULL,
    [PLAYER1SCORE] int  NOT NULL,
	[PLAYER2] varchar(max)  NOT NULL,
    [PLAYER2SCORE] int  NOT NULL,
    [STARTDATE] datetime  NOT NULL,
    [PLAYDATE] datetime  NOT NULL,
	[STATUS] int  NOT NULL,
	[TURN] int  NOT NULL,
	[HANDSYSTEM] varchar(max)  NOT NULL,
	[HANDSIZE] int  NOT NULL,
	[SCORINGSYSTEM] varchar(max)  NOT NULL,
	[LASTMOVE] varchar(max) NOT NULL,
	[HAND] varchar(max) NOT NULL,
	[STARTINGBOARD] varchar(max)  NOT NULL,
	[PLAYERBOARD] varchar(max)  NOT NULL,
	[MULTIPLIERS] varchar(max)  NOT NULL
);
GO

-- --------------------------------------------------
-- Creating all PRIMARY KEY constraints
-- --------------------------------------------------

-- Creating primary key on [GAMEID] in table 'Games'
ALTER TABLE [dbo].[Games]
ADD CONSTRAINT [PK_Games]
    PRIMARY KEY CLUSTERED ([GAMEID] ASC);
GO

-- --------------------------------------------------
-- Creating all FOREIGN KEY constraints
-- --------------------------------------------------

-- --------------------------------------------------
-- Script has ended
-- --------------------------------------------------