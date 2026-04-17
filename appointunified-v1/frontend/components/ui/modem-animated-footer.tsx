"use client";
import React from "react";
import Link from "next/link";
import {
  NotepadTextDashed,
  Twitter,
  Linkedin,
  Github,
  Mail,
  CalendarCheck
} from "lucide-react";
import { cn } from "@/lib/utils";

interface FooterLink {
  label: string;
  href: string;
}

interface SocialLink {
  icon: React.ReactNode;
  href: string;
  label: string;
}

interface FooterProps {
  brandName?: string;
  brandDescription?: string;
  socialLinks?: SocialLink[];
  navLinks?: FooterLink[];
  creatorName?: string;
  creatorUrl?: string;
  brandIcon?: React.ReactNode;
  className?: string;
}

export const Footer = ({
  brandName = "AppointUnified",
  brandDescription = "Your time, mathematically perfected. A unified engine across Healthcare, Government, and Services.",
  socialLinks = [],
  navLinks = [],
  creatorName,
  creatorUrl,
  brandIcon,
  className,
}: FooterProps) => {
  return (
    <section className={cn("relative w-full mt-0 overflow-hidden", className)}>
      <footer className="border-t border-border bg-transparent mt-20 relative">
        <div className="max-w-7xl flex flex-col justify-between mx-auto min-h-[30rem] sm:min-h-[35rem] md:min-h-[40rem] relative p-4 py-10 z-20">
          <div className="flex flex-col mb-12 sm:mb-20 md:mb-0 w-full">
            <div className="w-full flex flex-col items-center">
              <div className="space-y-2 flex flex-col items-center flex-1">
                <div className="flex items-center gap-2">
                  <span className="text-text-primary text-3xl font-bold tracking-tight">
                    {brandName}
                  </span>
                </div>
                <p className="text-text-secondary font-medium text-center w-full max-w-md px-4 sm:px-0">
                  {brandDescription}
                </p>
              </div>

              {socialLinks.length > 0 && (
                <div className="flex mb-8 mt-6 gap-4">
                  {socialLinks.map((link, index) => (
                    <Link
                      key={index}
                      href={link.href}
                      className="text-text-muted hover:text-accent transition-colors"
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      <div className="w-6 h-6 hover:scale-110 duration-300">
                        {link.icon}
                      </div>
                      <span className="sr-only">{link.label}</span>
                    </Link>
                  ))}
                </div>
              )}

              {navLinks.length > 0 && (
                <div className="flex flex-wrap justify-center gap-6 text-sm font-bold text-text-muted max-w-full px-4 mt-4 uppercase tracking-widest">
                  {navLinks.map((link, index) => (
                    <Link
                      key={index}
                      className="hover:text-accent duration-300"
                      href={link.href}
                    >
                      {link.label}
                    </Link>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="mt-20 md:mt-24 flex flex-col gap-2 md:gap-1 items-center justify-center md:flex-row md:items-center md:justify-between px-4 md:px-0 relative z-30">
            <p className="text-xs font-bold text-text-muted tracking-widest uppercase text-center md:text-left">
              ©{new Date().getFullYear()} {brandName.toUpperCase()}. ALL RIGHTS RESERVED.
            </p>
            {creatorName && creatorUrl && (
              <nav className="flex gap-4">
                <Link
                  href={creatorUrl}
                  target="_blank"
                  className="text-xs font-bold tracking-widest uppercase text-text-muted hover:text-accent transition-colors duration-300"
                >
                  Crafted by {creatorName}
                </Link>
              </nav>
            )}
          </div>
        </div>

        {/* Large background text - FIXED */}
        <div 
          className="bg-gradient-to-b from-text-primary/10 via-text-primary/5 to-transparent bg-clip-text text-transparent leading-none absolute left-1/2 -translate-x-1/2 bottom-40 md:bottom-24 font-extrabold tracking-tighter pointer-events-none select-none text-center px-4 z-0"
          style={{
            fontSize: 'clamp(3rem, 15vw, 12rem)',
            maxWidth: '100vw',
            whiteSpace: 'nowrap'
          }}
        >
          {brandName.toUpperCase()}
        </div>

        {/* Bottom line */}
        <div className="absolute bottom-28 sm:bottom-32 backdrop-blur-sm h-[1px] bg-gradient-to-r from-transparent via-accent/30 to-transparent w-full left-1/2 -translate-x-1/2 z-10"></div>
        
        {/* Bottom logo backdrop blur */}
        <div className="absolute hover:border-accent duration-400 drop-shadow-[0_0px_30px_rgba(129,140,248,0.2)] bottom-16 md:bottom-16 backdrop-blur-md rounded-3xl bg-white/40 left-1/2 border border-white/60 flex items-center justify-center p-3 -translate-x-1/2 z-20 shadow-soft">
          <div className="w-12 sm:w-16 md:w-20 h-12 sm:h-16 md:h-20 bg-gradient-to-br from-accent to-accent-light rounded-2xl flex items-center justify-center shadow-lg">
            {brandIcon || (
              <CalendarCheck className="w-8 sm:w-10 md:w-10 h-8 sm:h-10 md:h-10 text-white drop-shadow-md" />
            )}
          </div>
        </div>

        {/* Bottom shadow */}
        <div className="bg-gradient-to-t from-primary via-primary/80 blur-[2em] to-transparent absolute bottom-0 w-full h-32 pointer-events-none z-10"></div>
      </footer>
    </section>
  );
};
